package client

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, Unsubscribe}
import client.controller.Controller
import client.controller.Messages.ViewToClientMessages.{ChatMessage, JoinQueue, PlayAgain, UserExited, UserMadeHisMove, UserReadyToJoin, UsernameChosen}
import client.controller.MoveOutcome.ServerDown.{GameServerDown, GreetingServerDown}
import client.controller.MoveOutcome._
import model.Card
import shared.ChatMessages.{SendChatMessageToGameServer, SendOnChat}
import shared.ClientMoveAckType.{HandSwitchRequestAccepted, HandSwitchRequestRefused, PassAck, TimeoutAck, WordAccepted, WordRefused}
import shared.ClientToGameServerMessages.{ClientMadeMove, DisconnectionToGameServerNotification, EndTurnUpdateAck, GameEndedAck, MatchTopicListenAck, PlayerTurnBeginAck, SomeoneDisconnectedAck}
import shared.ClientToGreetingMessages._
import shared.GameServerToClientMessages.{ClientMoveAck, DisconnectionToGameServerNotificationAck, EndTurnUpdate, GameEnded, MatchTopicListenQuery, PlayerTurnBegins, SomeoneDisconnected}
import shared.{ClusterScheduler, CustomScheduler, Move}
import shared.Channels.GREETING_SERVER_RECEIVES_TOPIC
import shared.GreetingToClientMessages._

/** ClientActor è l'attore che impersona l'utente nel rapporto client-server
 *
 *  ClientActor si occupa nell'ordine di:
 *    - contattare un GreetingServer per poter partecipare ad una partita
 *    - gestire rapporti con GameServer in fase di gioco
 *    - gestire chiusura/interruzione partita
 *
 *  L'interazione con i server ha luogo in seguito a messaggi inviati a ClientActor da una UI
 *
 *  I messaggi provenienti dai Server possono portare a invocazione di metodi del Controller che
 *    gestirà il rapporto con una UI
 */
class ClientActor extends Actor{
  /** mediator che permette gestione topic */
  val mediator: ActorRef = DistributedPubSub.get(context.system).mediator
  private val cluster = Cluster.get(context.system)
  /** scheduler per un invio piu sicuro dei messaggi */
  val scheduler: CustomScheduler = ClusterScheduler(cluster)


  /** Option contenete ActorRef di GreetingServer */
  var greetingServerActorRef: Option[ActorRef] = None
  /** Option contenete ActorRef di GameServer */
  var gameServerActorRef: Option[ActorRef] = None
  /** Option contenete topic su cui GameServer fa broadcast */
  var gameServerTopic: Option[String] = None
  /** Option contenete topic su cui GameServer fa broadcast per messaggi chat */
  var chatTopic: Option[String] = None
  /** Option contenete username scelto dall'utente */
  var username: Option[String] = None
  /** utente è pronto per giocare */
  var playerIsReady:Boolean = false

  mediator ! Subscribe(GREETING_SERVER_RECEIVES_TOPIC, self)

  override def receive: Receive = waitingUsernameFromUser


  /** Stato di attesa Username
   *  - Attende messaggio contente username,
   *      memorizza username,
   *      richiede al controller aggiornamento UI
   *  - Attende eventuale messaggio di chiusura della UI
   *
   *  @return stato ClientActor
   */
  def waitingUsernameFromUser: Receive = {
    case message : UsernameChosen =>
      username = Some(message.username)
      Controller.onLoginResponse()
      context.become(waitingUserQueueRequest)

    case _:UserExited => stopSelf()
  }


  /** Stato attesa richiesta di gioco
   *
   *  - Attende richiesta di gioco da parte dell'utente
   *  - Attende eventuale messaggio di chiusura della UI
   *
   *  @return stato ClientActor
   */
  def waitingUserQueueRequest: Receive = {
    case _: JoinQueue =>
      scheduler.replaceBehaviourAndStart(()=>estabilishConnectionToGreetingServer())
      context.become(waitingAckOnUserQueueRequest)

    case _:UserExited => stopSelf()
  }



  /** Stato attesa conferma connessione al GreetingServer
   *
   *  - Attende risposta di GreetingServer alla richiesta el giocatore di essere messo in coda per giocare
   *      + se la richiesta fallisce gestisce chiusura gioco
   *      + se la richiesta va a buon fine si mette in ascolto dell'evento crollo del server
   *  - Attende eventuale messaggio di chiusura di UI o crollo Server
   *
   *  @return stato ClientActor
   */
  def waitingAckOnUserQueueRequest: Receive = UnexpectedShutdown orElse {
    case connection: ConnectionAnswer =>
      scheduler.stopTask()
      if (connection.connected) {
        greetingServerActorRef = Some(sender)
        context.watch(sender) //mi metto in ascolto di una eventuale morte del GreetingServer
        context.become(waitingReadyToJoinRequestFromGreetingServer)
      } else {
        //comunicare al player che la connessione non può essere stabilita e chiudere (valutare prossime due istruzioni)
        Controller.onConnectionFailed()
        context.stop(self)
      }
  }


  /** Stato attesa richiesta partecipazione a partita
   *
   *  - Inoltra all'UI richiesta di partecipare ad una partita
   *  - Attende eventuale messaggio di chiusura di UI o crollo Server
   *
   *  @return stato ClientActor
   */
  def waitingReadyToJoinRequestFromGreetingServer: Receive = UnexpectedShutdown orElse {
    case _: ReadyToJoinQuery =>
      Controller.askUserToJoinGame() //chiedo all'UI di chiedere all'utente se è ancora disposto a giocare
      context.become(waitingReadyToJoinAnswerFromUser)
  }


  /** Stato attesa risposta dell'utente per partecipazione a partita
   *
   *  - Inoltra a GreetingServer risposta di partecipare ad una partita,
   *      aggiorna campo playerIsReady
   *  - Attende eventuale messaggio di chiusura di UI o crollo Server
   *
   *  @return stato ClientActor
   */
  def waitingReadyToJoinAnswerFromUser:Receive = UnexpectedShutdown orElse {
    case message: UserReadyToJoin =>
      //memorizzo risposta dell'utente per valutare in futuro cosa sia necessario gestire
      playerIsReady = message.ready
      //comunico al greeting la disponibilità dell'utante
      scheduler.replaceBehaviourAndStart(() => responseToJoinMatchRequest())
      context.become(waitingReadyToJoinAckFromGreetingServer)
  }


  /** Stato attesa ack ricezione risposta utente per partecipazione a partita
   *
   *  - Se l'utente partecipa alla partita (playerIsReady)
   *      si mette in attesa di messaggi dal GameServer
   *  - Se l'utente non partecipa torna nello stato di attesa richiesta
   *      di mettersi in coda per una partita
   *  - Attende eventuale messaggio di chiusura di UI o crollo Server
   *
   *  @return stato ClientActor
   */
  def waitingReadyToJoinAckFromGreetingServer: Receive = UnexpectedShutdown orElse {
    case _: ReadyToJoinAck =>
      scheduler.stopTask()
      //valuto risposta del giocatore contenuta in playerIsReady
      if (playerIsReady) {
        //giocatore pronto per giocare
        context.become(waitingGameServerTopic)
      } else {
        //giocatore non pronto alla partita
        resetMatchInfo() //per sicurezza resetto le informazioni temporanee della partita
        context.become(waitingUserQueueRequest)
      }
  }


  /** Stato attesa primo messaggio da GameServer
   *
   *  - Attende dal GameServer messaggio con tutti i parametri necessari ad impostare la partita
   *      + memorizza ActorRef del GameServer, i topic
   *      + si mette in ascolto di eventuale morte GameServer
   *      + inoltra informazioni alla UI
   *  - Attende eventuale messaggio di chiusura di UI o crollo Server
   *  - Attende eventuale messaggio di terminazione partita per abbandono di un avversario
   *
   *  @return stato ClientActor
   */
  def waitingGameServerTopic: Receive = UnexpectedShutdown orElse
    opponentLefted orElse {
    case topicMessage: MatchTopicListenQuery =>
      updateGameServerReference(sender())
      updateGameServerTopic(topicMessage.gameServerTopic)
      updateChatTopic(topicMessage.gameChatTopic)
      Controller.onMatchStart(topicMessage.playerHand, topicMessage.playersList)
      sendGameServerTopicReceived()
      context.become(waitingInTurnPlayerNomination)
  }


  /** Stato attesa comunicazione giocatore in curno
   *
   *  - Ricevuto il giocatore in turno valuta se:
   *      + è il turno proprio -> richiede all'UI una mossa dell'utente
   *      + è dell'avversario -> attende terminazione turno avversario
   *  - Attende eventuale messaggio di chiusura di UI o crollo Server
   *  - Attende eventuale messaggio di terminazione partita per abbandono di un avversario
   *  - Attende eventuali messaggi chat
   *
   *  @return stato ClientActor
   */
  def waitingInTurnPlayerNomination: Receive =
    UnexpectedShutdown orElse
    opponentLefted orElse
      waitingChatMessages orElse {
    case message: PlayerTurnBegins =>
      if (message.playerInTurn == self){
        //caso in cui spetta a me giocare
        Controller.userTurnBegins()
        context.become(waitingUserMakingMove)
      } else {
        // è il turno di un avversario, devo mettermi in attesa degli aggiornamenti di fine turno
        context.become(waitingTurnEndUpdates)
      }
      sendPlayerInTurnAck() //invio ack al GameServer
    case message: GameEnded =>
      sendGameEndedAck()
      Controller.matchEnded(message.name, message.actorRef==self)
      resetMatchInfo()
      context.become(waitingUserChoosingWheterPlayAgainOrClosing)
    }


  /** Stato attesa mossa utente
   *
   *  - Attende mossa dell'utente e la inoltra al GameServer
   *  - Attende eventuale messaggio di chiusura di UI o crollo Server
   *  - Attende eventuale messaggio di terminazione partita per abbandono di un avversario
   *
   *  @return stato ClientActor
   */
  def waitingUserMakingMove: Receive = UnexpectedShutdown orElse
    opponentLefted orElse
    waitingChatMessages orElse {
    case message :UserMadeHisMove =>
      scheduler.replaceBehaviourAndStart(()=>sendUserMove(message.move))
      context.become(waitingMoveAckFromGameServer)
  }

  /** Stato attesa ack ricezione mossa utente dal GameServer
   *
   *  - Gestisce ack della mossa utente: la mossa può
   *      + essere accettata -> si passa all'attesa degli aggiornamenti di fineturno
   *      + essere rifiutata -> l'utente puo effettuare una nuova mossa
   *  - Attende eventuale messaggio di chiusura di UI o crollo Server
   *  - Attende eventuale messaggio di terminazione partita per abbandono di un avversario
   *
   *  @return stato ClientActor
   */
  def waitingMoveAckFromGameServer: Receive =  UnexpectedShutdown orElse
    opponentLefted orElse
    waitingChatMessages orElse {
    case serverAnswer: ClientMoveAck =>
      scheduler.stopTask()
      serverAnswer.moveAckType match {
        case wordAccepted: WordAccepted => onWordAccepted(wordAccepted.hand)
        case _: WordRefused => onWordRefused()
        case handSwitchAccepted: HandSwitchRequestAccepted => onHandSwitchAccepted(handSwitchAccepted.hand)
        case _: HandSwitchRequestRefused => onHandSwitchRefused()
        case _: PassAck => onPassAck()
        case _: TimeoutAck => onTimeoutAck()
      }
  }



  /** Stato attesa aggiornamenti di fineturno da GameServer
   *
   *  - Inoltra gli aggiornamenti alla UI
   *  - Attende eventuale messaggio di chiusura di UI o crollo Server
   *  - Attende eventuale messaggio di terminazione partita per abbandono di un avversario
   *
   *  @return stato ClientActor
   */
  def waitingTurnEndUpdates: Receive = UnexpectedShutdown orElse
    opponentLefted orElse
    waitingChatMessages orElse {
    case message :EndTurnUpdate =>
      Controller.turnEndUpdates(message.playersRanking, message.board)
      sendEndTurnUpdateAck()
      context.become(waitingInTurnPlayerNomination)
  }


  /** Stato post-partita attesa decisione utente di giocare nuova partita o uscire dal gioco
   *
   *  - utente decide:
   *      + giocare nuovamente -> si torna in attesa richiesta utente per coda di gioco
   *      + disconnessione -> invio messaggio di disconnessione al GreetingServer
   *  - Attende eventuale messaggio di chiusura di UI o crollo Server
   *  - Attende eventuale messaggio di terminazione partita per abbandono di un avversario
   *
   *  @return stato ClientActor
   */
  def waitingUserChoosingWheterPlayAgainOrClosing: Receive = UnexpectedShutdown orElse
    waitingChatMessages orElse {
    case message: PlayAgain =>
      if (message.userWantsToPlay) {
        resetMatchInfo()
        context.become(waitingUserQueueRequest)
      } else {
        scheduler.replaceBehaviourAndStart(() => tearDownConnectionToGreetingServer())
        context.become(waitingDisconnectionAck)
      }
  }

  /** attendo che GreetingServer confermi ricezione messaggio di disconnessione */
  def waitingDisconnectionAck: Receive = waitingChatMessages orElse {
    case _: DisconnectionToGameServerNotificationAck => handleClientStop()

    case _: DisconnectionAck => handleClientStop()
  }


  /** gestisco stop dell'attore ClientActor */
  def handleClientStop():Unit = {
    scheduler.stopTask()
    Controller.exit()
    context.stop(self)
  }


  /** Stato per la gestione dei messaggi chat
   *
   *  - ricezione ChatMessage: l'utente vuole inviare questo messaggio a avversari;
   *      il messaggio viene inoltrato a GameServer che lo invierà in broadcast
   *  - ricezione SendOnChat: ricezione messaggio broadcast
   *      se proviene da un avversario viene inviato all' UI per essere mostrato in chat
   *
   *  @return stato ClientActor
   */
  def waitingChatMessages: Receive = {
    case chatMessage: ChatMessage => gameServerActorRef.get ! SendChatMessageToGameServer(username.getOrElse("Username Sconosciuto"), chatMessage.message)
    case sendOnChatMessage: SendOnChat =>
      if (sendOnChatMessage.senderActor != self){
        Controller.showInChat(sendOnChatMessage.senderUsername, sendOnChatMessage.message)
      }
  }


  //GESTIONE MESSAGGI GAME_SERVER

  //memorizza l'actorRef del GameServer e si registra per esser informato di un suo crollo
  private def updateGameServerReference(gameServer: ActorRef): Unit ={
    gameServerActorRef = Some(gameServer)
    context.watch(gameServer)
  }

  //memorizza il topic relativo al GameServer e si registra per esser informato di un suo crollo
  private def updateGameServerTopic(topic: String): Unit ={
    gameServerTopic = Some(topic)
    mediator ! Subscribe(gameServerTopic.get, self)
  }

  //memorizza il topic relativo alla chat e si registra per poter ricevere messaggi degli altri giocatori
  private def updateChatTopic(topic: String): Unit = {
    chatTopic = Some(topic)
    mediator ! Subscribe(chatTopic.get, self)
  }

  //confermo a GameServer ricezione messaggio MatchTopicListenQuery
  private def sendGameServerTopicReceived(): Unit = {
    gameServerActorRef.get ! MatchTopicListenAck()
  }

  //confermo a GameServer ricezione PlayerTurnBegins message
  private def sendPlayerInTurnAck(): Unit = {
    gameServerActorRef.get ! PlayerTurnBeginAck()
  }


  //inoltro la mossa scelta dall'utente al GameServer
  private def sendUserMove(move:Move): Unit = {
    gameServerActorRef.get ! ClientMadeMove(move)
  }

  //confermo a GameServer ricezione EndTurnUpdate message
  private def sendEndTurnUpdateAck(): Unit = {
    gameServerActorRef.get ! EndTurnUpdateAck()
  }




  /** Il GameServer ha accettato la parola composta dall'utente, devo:
   *  - smettere di inviare la mossa fatta dall'utente al GameServer
   *  - comunicare la nuova mano dell'utente alla UI perchè venga visualizzata
   *  - passare allo stato in cui attendo update di fine turno
   *  @param hand = è l'insieme delle carte che compongono la mano dell'utente
   */
  private def onWordAccepted(hand:Vector[Card]):Unit = {
    Controller.moveOutcome(AcceptedWord(hand))
    context.become(waitingTurnEndUpdates)
  }


  /** Il GameServer NON ha accettato la parola composta dall'utente, devo:
   *  - smettere di inviare la mossa fatta dall'utente al GameServer
   *  - comunicare a UI il fallimento
   *  - passare allo stato in cui una nuova mossa dall'utente
   */
  private def onWordRefused():Unit = {
    Controller.moveOutcome(RefusedWord())
    context.become(waitingUserMakingMove)
  }


  /** Il GameServer ha accettato la richiesta di sostituzione della mano fatta dall'utente, devo:
   *  - smettere di inviare la mossa fatta dall'utente al GameServer
   *  - comunicare la nuova mano dell'utente all'UI perchè venga visualizzata
   *  - passare allo stato in cui attendo update di fine turno
   *  @param hand = è l'insieme delle carte che compongono la mano dell'utente
   */
  private def onHandSwitchAccepted(hand:Vector[Card]):Unit = {
    Controller.moveOutcome(HandSwitchAccepted(hand))
    context.become(waitingTurnEndUpdates)
  }


  /** Il GameServer non ha accettato la richiesta di sostituzione della mano fatta dall'utente, devo:
   *  - smettere di inviare la mossa fatta dall'utente al GameServer
   *  - comunicare all'UI il fallimento
   *  - passare allo stato in cui una nuova mossa dall'utente
   */
  private def onHandSwitchRefused():Unit = {
    Controller.moveOutcome(HandSwitchRefused())
    context.become(waitingUserMakingMove)
  }


  /** Il GameServer ha accettato la richiesta di passare il turno fatta dall'utente, devo:
   *  - smettere di inviare la mossa fatta dall'utente al GameServer
   *  - comunicare all'UI che tutto è andato a buon fine
   *  - passare allo stato in cui attendo update di fine turno
   */
  private def onPassAck():Unit = {
    Controller.moveOutcome(PassReceived())
    context.become(waitingTurnEndUpdates)
  }


  /** Il GameServer ha ricevuto la notifica di timeout, devo:
   *  - smettere di inviare la notifica fatta dall'utente al GameServer
   *  - comunicare all'UI che tutto è andato a buon fine
   *  - passare allo stato in cui attendo update di fine turno
   */
  private def onTimeoutAck():Unit = {
    Controller.moveOutcome(TimeoutReceived())
    context.become(waitingTurnEndUpdates)
  }

  //comunico a GameServer ricezione del messaggio di terminazione della partita
  private def sendGameEndedAck(): Unit = {
    gameServerActorRef.get !  GameEndedAck()
  }




  //GESTIONE COLLOQUIO CON GREETING_SERVER

  //stabilisce la connessione con il greeting server
  private def estabilishConnectionToGreetingServer(): Unit = {
    val DEFAULT_NAME:String = "sconosciuto"
    mediator ! Publish(GREETING_SERVER_RECEIVES_TOPIC, ConnectionToGreetingQuery(username.getOrElse(DEFAULT_NAME))) //questo deve rimanere su topic
  }

  //utente conferma di voler partecipare alla partita o meno
  private def responseToJoinMatchRequest():Unit = {
    greetingServerActorRef.get ! PlayerReadyAnswer(playerIsReady)
  }

  //invia al GreetingServer una notifica di disconnessione
  private def tearDownConnectionToGreetingServer(): Unit = {
    greetingServerActorRef.get ! DisconnectionToGreetingNotification()
  }



  //GESTIONE DELLA DISCONNESSIONE DAI SERVER O DISCONNESSIONE DI UTENTE/AVVERSARIO

  //gestisce disconnessione di un avversario
  private def opponentLefted: Receive = {
    case _: SomeoneDisconnected =>
      sendAckOnOpponentDisconnection()
      resetMatchInfo()
      Controller.playerLeft()
      context.become(waitingUserQueueRequest)
  }

  //invio conferma ricezione notifica avversario disconnesso
  private def sendAckOnOpponentDisconnection(): Unit = {
    gameServerActorRef.get ! SomeoneDisconnectedAck()
  }


  //controllo se l'utente ha effettuato uno shutdown forzato dell'applicazione
  private def UnexpectedShutdown: Receive = {
    case _:UserExited => onUserExited()
    case deathMessage:Terminated => handleServersUnexpectedShutdown(deathMessage.actor)
  }

  //dice se l'utente sta giocando, l'idea è che se è in partita allora ci sarà un GameServer registrato
  private def isPaying:Boolean = gameServerActorRef.nonEmpty

  //gestione dell'uscita forzata dell'utente
  private def onUserExited() : Unit = {
    if (isPaying) {
      notifyDisconnectionToGameServer()
    } else {
      notifyDisconnectionToGreetingServer()
    }
    context.become(waitingDisconnectionAck)
  }

  //l'attore client termina se stesso
  private def stopSelf(): Unit = {
    context.stop(self)
  }

  //chiamato dopo che l'utente si è disconnesso inaspettatamente e non era in partita => comunico il fatto a GreetingServer
  private def notifyDisconnectionToGreetingServer(): Unit = {
    scheduler.replaceBehaviourAndStart(()=>tearDownConnectionToGreetingServer())
  }

  //chiamato dopo che l'utente si è disconnesso inaspettatamente ed era in partita => comunico il fatto a GameServer
  private def notifyDisconnectionToGameServer(): Unit = {
    scheduler.replaceBehaviourAndStart(()=>tearDownConnectionToGameServer())
  }


  //invio notifica di disconnessione al GameServer in seguito a UI chiusa forzatamente
  private def tearDownConnectionToGameServer(): Unit = {
    gameServerActorRef.get ! DisconnectionToGameServerNotification()
  }


  //verifico se l'attore che è crollato è un server a me collegato
  private def handleServersUnexpectedShutdown(serverDown: ActorRef): Unit = {
    if(serverDown == greetingServerActorRef.get){
      handleGreetingServerDisconnection()
    }else if (serverDown==gameServerActorRef.get){
      handleGameServerDisconnection()
    }else {
      //è stato stoppato un attore con cui non ho rapporti
    }
  }

  //se crolla il greeting comunico alla UI e mi stoppo tanto non c'è piu niente da fare
  private def handleGreetingServerDisconnection(): Unit = {
    //comunicare alla UI la morte del Greeting server, valutare se adottare un comportamento differente
    Controller.serversDown(GreetingServerDown())
    context.stop(self)
  }

  //gestisco il caso in cui crolla il GameServer
  private def handleGameServerDisconnection(): Unit = {
    if (greetingServerActorRef.nonEmpty) {
      //comunicare alla UI la morte del Game server, valutare se adottare un comportamento differente
      Controller.serversDown(GameServerDown())
      resetMatchInfo()
      //necessario saltare in uno stato in cui faccio scegliere a player che fare: se continuar a giocare o meno
      context.become(waitingUserQueueRequest)
    }
  }

  /** resetta le variabili temporanee */
  def resetMatchInfo():Unit = {
    if(gameServerActorRef.isDefined){
      context.unwatch(gameServerActorRef.get)
    }
    if (gameServerTopic.isDefined){
      mediator ! Unsubscribe(gameServerTopic.get, self)
    }
    if (chatTopic.isDefined){
      mediator ! Unsubscribe(chatTopic.get, self)
    }
    chatTopic = None
    gameServerTopic = None
    gameServerActorRef = None
    playerIsReady = false
  }
}

/** Permette di creare l'attore */
object ClientActor{
  def props(): Props = Props(new ClientActor())
}
