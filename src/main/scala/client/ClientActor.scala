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
import shared.Topic.GREETING_SERVER_RECEIVES_TOPIC
import shared.GreetingToClientMessages._

import scala.collection.mutable.ArrayBuffer

class ClientActor extends Actor{
  val mediator = DistributedPubSub.get(context.system).mediator
  private val cluster = Cluster.get(context.system)
  val scheduler: CustomScheduler = ClusterScheduler(cluster)


  //contiene l'ActorRef del server
  var greetingServerActorRef: Option[ActorRef] = None
  //contiene l'ActorRef del server
  var gameServerActorRef: Option[ActorRef] = None
  //contiene il topic relativi al GameServer
  var gameServerTopic: Option[String] = None
  //contiene il topic della chat
  var chatTopic: Option[String] = None
  //contiene lo username scelto dall'utente
  var username: Option[String] = None
  //l'utente è disposto a giocare
  var playerIsReady:Boolean = false

  mediator ! Subscribe(GREETING_SERVER_RECEIVES_TOPIC, self)

  override def receive: Receive = waitingUsernameFromUser


  //attendo che l'utente scelga il proprio nome dopodichè lo memorizzo
  def waitingUsernameFromUser: Receive = {
    case message : UsernameChosen => {
      username = Some(message.username)
      Controller.onLoginResponse()
      context.become(waitingUserQueueRequest)
    }
    case _:UserExited => stopSelf()
  }

  // attendo che giocatore richieda di giocare nuova partita per contattare il server
  def waitingUserQueueRequest: Receive = {
    case _: JoinQueue => {
      scheduler.replaceBehaviourAndStart(()=>estabilishConnectionToGreetingServer())
      context.become(waitingAckOnUserQueueRequest)
    }
    case _:UserExited => stopSelf()
  }


  //attendo che il Greeting risponda alla richiesta del client di esser messo in coda
  def waitingAckOnUserQueueRequest: Receive = UnexpectedShutdown orElse {
    case connection: ConnectionAnswer => {
      scheduler.stopTask()
      connection.connected match {
        case true =>{
          greetingServerActorRef = Some(sender)
          context.watch(sender) //mi metto in ascolto di una eventuale morte del GreetingServer
          context.become(waitingReadyToJoinRequestFromGreetingServer)
        }
        case false => {
          //comunicare al player che la connessione non può essere stabilita e chiudere (valutare prossime due istruzioni)
          Controller.onConnectionFailed()
          context.stop(self)
        }
      }
    }
  }


  //attendo richiesta di join partita da parte del greeting server
  def waitingReadyToJoinRequestFromGreetingServer: Receive = UnexpectedShutdown orElse {
    case _: ReadyToJoinQuery => {
      Controller.askUserToJoinGame()//chiedo all'UI di chiedere all'utente se è ancora disposto a giocare
      context.become(waitingReadyToJoinAnswerFromUser)
    }
  }


  //in attesa che l'utente risponda se è pronto
  def waitingReadyToJoinAnswerFromUser:Receive = UnexpectedShutdown orElse {
    case message: UserReadyToJoin => {
      //memorizzo risposta dell'utente per valutare in futuro cosa sia necessario gestire
      playerIsReady = message.ready
      //comunico al greeting la disponibilità dell'utante
      scheduler.replaceBehaviourAndStart(() => responseToJoinMatchRequest())
      context.become(waitingReadyToJoinAckFromGreetingServer)
    }
  }




  //attendo che GreetingServer confermi ricezione di join partita da parte del greeting server
  def waitingReadyToJoinAckFromGreetingServer: Receive = UnexpectedShutdown orElse {
    case _: ReadyToJoinAck => {
      scheduler.stopTask()
      //valuto risposta del giocatore contenuta in playerIsReady
      playerIsReady match {
        case true => {
          //giocatore pronto per giocare
          context.become(waitingGameServerTopic)
        }
        case false => {
          //giocatore non pronto alla partita
          resetMatchInfo() //per sicurezza resetto le informazioni temporanee della partita
          Controller.onLoginResponse()
          context.become(waitingUserQueueRequest)
        }
      }
    }
  }

  //attendo che il GameServer mi invii il messaggio con le informazioni per impostare la partita lato client
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

  //attendo che il GameServer decida di chi è il turno
  def waitingInTurnPlayerNomination: Receive =
    UnexpectedShutdown orElse
    opponentLefted orElse
      waitingChatMessages orElse {
    case message: PlayerTurnBegins => {
      if (message.playerInTurn == self){
        //caso in cui spetta a me giocare
        Controller.userTurnBegins()
        context.become(waitingUserMakingMove)
      } else {
        // è il turno di un avversario, devo mettermi in attesa degli aggiornamenti di fine turno
        context.become(waitingTurnEndUpdates)
      }
      sendPlayerInTurnAck() //invio ack al GameServer
    }
    case message: GameEnded => {
      sendGameEndedAck()
      Controller.matchEnded(message.name, message.actorRef==self)
      resetMatchInfo()
      context.become(waitingUserChoosingWheterPlayAgainOrClosing)
    }
  }



  //attendo che l'utente faccia la sua mossa per poi comunicarla al GameServer
  def waitingUserMakingMove: Receive = UnexpectedShutdown orElse
    opponentLefted orElse
    waitingChatMessages orElse {
    case message :UserMadeHisMove =>{
      scheduler.replaceBehaviourAndStart(()=>sendUserMove(message.move))
      context.become(waitingMoveAckFromGameServer)
    }
  }

  //attendo che il server mi confermi la ricezione della mossa
  def waitingMoveAckFromGameServer: Receive =  UnexpectedShutdown orElse
    opponentLefted orElse
    waitingChatMessages orElse {
    case serverAnswer: ClientMoveAck => {
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
  }


  //attendo che il GameServer comunichi gli aggiornamenti da compiere
  def waitingTurnEndUpdates: Receive = UnexpectedShutdown orElse
    opponentLefted orElse
    waitingChatMessages orElse {
    case message :EndTurnUpdate =>{
      Controller.turnEndUpdates(message.playersRanking, message.board)
      sendEndTurnUpdateAck()
      context.become(waitingInTurnPlayerNomination)
    }
  }


  //stato in cui attendo che il controller mi comunichi se l'utente vuole giocare una nuova partita o uscire
  def waitingUserChoosingWheterPlayAgainOrClosing: Receive = UnexpectedShutdown orElse
    waitingChatMessages orElse {
    case message: PlayAgain => {
      message.userWantsToPlay match {
        case true => {
          resetMatchInfo()
          //Controller.onLoginResponse()
          context.become(waitingUserQueueRequest)
        }
        case false => {
          scheduler.replaceBehaviourAndStart(()=>tearDownConnectionToGreetingServer())
          context.become(waitingDisconnectionAck)
        }
      }
    }
  }

  //attendo che GreetingServer confermi ricezione messaggio di disconnessione
  def waitingDisconnectionAck: Receive = waitingChatMessages orElse {
    case _: DisconnectionToGameServerNotificationAck => {
      handleClientStop()
    }

    case _: DisconnectionAck => {
      handleClientStop()
    }
  }


  //GESTIONE STOP CLIENT
  def handleClientStop():Unit = {
    scheduler.stopTask()
    //dovrò comunicare al controller la riuscita terminazione
    Controller.exit()
    context.stop(self)
  }





  //usato dai client in qualsiasi momento per attendere messaggi da altri client e da View
  def waitingChatMessages: Receive = {
    case chatMessage: ChatMessage => gameServerActorRef.get ! SendChatMessageToGameServer(username.getOrElse("Username Sconosciuto"), chatMessage.message)
    case sendOnChatMessage: SendOnChat => {
      if (sendOnChatMessage.senderActor != self){
        Controller.showInChat(sendOnChatMessage.senderUsername, sendOnChatMessage.message)
      }
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



  /*Il GameServer ha accettato la parola composta dall'utente, devo:
* - smettere di inviare la mossa fatta dall'utente al GameServer
* - comunicare la nuova mano dell'utente alla UI perchè venga visualizzata
* - passare allo stato in cui attendo update di fine turno
* */
  def onWordAccepted(hand:ArrayBuffer[Card]):Unit = {
    Controller.moveOutcome(AcceptedWord(hand))
    context.become(waitingTurnEndUpdates)
  }

  /*Il GameServer NON ha accettato la parola composta dall'utente, devo:
  * - smettere di inviare la mossa fatta dall'utente al GameServer
  * - comunicare a UI il fallimento
  * - passare allo stato in cui una nuova mossa dall'utente
  * */
  def onWordRefused():Unit = {
    Controller.moveOutcome(RefusedWord())
    context.become(waitingUserMakingMove)
  }

  /*Il GameServer ha accettato la richiesta di sostituzione della mano fatta dall'utente, devo:
  * - smettere di inviare la mossa fatta dall'utente al GameServer
  * - comunicare la nuova mano dell'utente all'UI perchè venga visualizzata
  * - passare allo stato in cui attendo update di fine turno
  * */
  def onHandSwitchAccepted(hand:ArrayBuffer[Card]):Unit = {
    Controller.moveOutcome(HandSwitchAccepted(hand))
    context.become(waitingTurnEndUpdates)
  }

  /*Il GameServer non ha accettato la richiesta di sostituzione della mano fatta dall'utente, devo:
  * - smettere di inviare la mossa fatta dall'utente al GameServer
  * - comunicare all'UI il fallimento
  * - passare allo stato in cui una nuova mossa dall'utente
  * */
  def onHandSwitchRefused():Unit = {
    Controller.moveOutcome(HandSwitchRefused())
    context.become(waitingUserMakingMove)
  }

  /*Il GameServer ha accettato la richiesta di passare il turno fatta dall'utente, devo:
  * - smettere di inviare la mossa fatta dall'utente al GameServer
  * - comunicare all'UI che tutto è andato a buon fine
  * - passare allo stato in cui attendo update di fine turno
  * */
  def onPassAck():Unit = {
    Controller.moveOutcome(PassReceived())
    context.become(waitingTurnEndUpdates)
  }

  /*Il GameServer ha ricevuto la notifica di timeout, devo:
  * - smettere di inviare la notifica fatta dall'utente al GameServer
  * - comunicare all'UI che tutto è andato a buon fine
  * - passare allo stato in cui attendo update di fine turno
  * */
  def onTimeoutAck():Unit = {
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












  //GESTIONE DELLA DISCONNESSIONE DAI SERVER O DISCONNESSIONE DELL'UTENTE

  private def opponentLefted: Receive = {
    case _: SomeoneDisconnected => {
      sendAckOnOpponentDisconnection()
      resetMatchInfo()
      Controller.playerLeft()
      context.become(waitingUserQueueRequest)
    }
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
    isPaying match {
      case true => notifyDisconnectionToGameServer
      case _    => notifyDisconnectionToGreetingServer
    }
    context.become(waitingDisconnectionAck)
  }

  //l'attore client termina se stesso
  private def stopSelf(): Unit = {
    context.stop(self)
  }

  //chiamato dopo che l'utente si è disconnesso inaspettatamente e non era in partita => comunico il fatto a GreetingServer
  def notifyDisconnectionToGreetingServer: Unit = {
    scheduler.replaceBehaviourAndStart(()=>tearDownConnectionToGreetingServer())
  }

  //chiamato dopo che l'utente si è disconnesso inaspettatamente ed era in partita => comunico il fatto a GameServer
  def notifyDisconnectionToGameServer: Unit = {
    scheduler.replaceBehaviourAndStart(()=>tearDownConnectionToGameServer())
  }


  //invio notifica di disconnessione al GameServer in seguito a UI chiusa forzatamente
  private def tearDownConnectionToGameServer(): Unit = {
    gameServerActorRef.get ! DisconnectionToGameServerNotification()
  }


  //verifico se l'attore che è crollato è un server a me collegato
  private def handleServersUnexpectedShutdown(serverDown: ActorRef): Unit = {
    if(serverDown == greetingServerActorRef.get){
      handleGreetingServerDisconnection
    }else if (serverDown==gameServerActorRef.get){
      handleGameServerDisconnection
    }else {
      //è stato stoppato un attore con cui non ho rapporti
    }
  }

  //se crolla il greeting comunico alla UI e mi stoppo tanto non c'è piu niente da fare
  private def handleGreetingServerDisconnection: Unit = {
    //comunicare alla UI la morte del Greeting server, valutare se adottare un comportamento differente
    Controller.serversDown(GreetingServerDown())
    context.stop(self)
  }

  //gestisco il caso in cui crolla il GameServer
  private def handleGameServerDisconnection: Unit = {
    if (greetingServerActorRef.nonEmpty) {
      //comunicare alla UI la morte del Game server, valutare se adottare un comportamento differente
      Controller.serversDown(GameServerDown())
      resetMatchInfo()
      //necessario saltare in uno stato in cui faccio scegliere a player che fare: se continuar a giocare o meno
      context.become(waitingUserQueueRequest)
    }
  }

  //resetta le variabili temporanee
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

object ClientActor{
  def props() = Props(classOf[ClientActor])
}
