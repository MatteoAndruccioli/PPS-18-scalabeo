package client

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, Unsubscribe}
import client.controller.Controller
import client.controller.Messages.ViewToClientMessages.{JoinQueue, PlayAgain, UserExited, UserMadeHisMove, UserReadyToJoin, UsernameChosen}
import client.controller.MoveOutcome.ServerDown.{GameServerDown, GreetingServerDown}
import client.controller.MoveOutcome._
import model.Card
import shared.ClientMoveAckType.{HandSwitchRequestAccepted, HandSwitchRequestRefused, PassAck, TimeoutAck, WordAccepted, WordRefused}
import shared.ClientToGameServerMessages.{ClientMadeMove, DisconnectionToGameServerNotification, EndTurnUpdateAck, GameEndedAck, MatchTopicListenAck, PlayerTurnBeginAck, SomeoneDisconnectedAck}
import shared.ClientToGreetingMessages._
import shared.GameServerToClientMessages.{ClientMoveAck, DisconnectionToGameServerNotificationAck, EndTurnUpdate, GameEnded, MatchTopicListenQuery, PlayerTurnBegins, SomeoneDisconnected}
import shared.{ClusterScheduler, CustomScheduler, Move}
import shared.Topic.GREETING_SERVER_RECEIVES_TOPIC
import shared.GreetingToClientMessages._

import scala.collection.mutable.ArrayBuffer

class ClientActor extends Actor{
  private val mediator = DistributedPubSub.get(context.system).mediator
  private val cluster = Cluster.get(context.system)
  private val scheduler: CustomScheduler = ClusterScheduler(cluster)

  //todo cambia nomi ai due option che tengono i nomi dei server che non si possono vedere
  //contiene l'ActorRef del server
  private var greetingServerActorRef: Option[ActorRef] = None
  //contiene l'ActorRef del server
  var gameServerActorRef: Option[ActorRef] = None
  //contiene il topic relativi al GameServer
  var gameServerTopic: Option[String] = None
  //contiene il topic della chat
  var chatTopic: Option[String] = None
  //contiene lo username scelto dall'utente
  private var username: Option[String] = None
  //l'utente è disposto a giocare
  private var playerIsReady:Boolean = false

  mediator ! Subscribe(GREETING_SERVER_RECEIVES_TOPIC, self)

  override def receive: Receive = waitingUsernameFromUser


  //attendo che l'utente scelga il proprio nome dopodichè lo memorizzo
  def waitingUsernameFromUser: Receive = {
    case message : UsernameChosen => {
      username = Some(message.username)
      println(self + " ricevuto messaggio con username da MainMenu" + username.getOrElse("errore-username !!!!"))
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
          println("Client " + self + " ricevuto ConnectionAnswer positiva ["+ connection.connected +"] dal GreetingServer "+ greetingServerActorRef.get)
          context.become(waitingReadyToJoinRequestFromGreetingServer)
        }
        case false => {
          //comunicare al player che la connessione non può essere stabilita e chiudere (valutare prossime due istruzioni)
          Controller.onConnectionFailed()
          context.stop(self)
          println("Client " + self + " ricevuto ConnectionAnswer negativa ["+ connection.connected +"] dal GreetingServer "+ greetingServerActorRef.get)
        }
      }
    }
  }


  //attendo richiesta di join partita da parte del greeting server
  def waitingReadyToJoinRequestFromGreetingServer: Receive = UnexpectedShutdown orElse {
    case _: ReadyToJoinQuery => {
      println("--------------------------------------------------------------------")
      println(self + " - Ricevuto richiesta di join match dal GreetingServer = " +sender())
      Controller.askUserToJoinGame()//chiedo all'UI di chiedere all'utente se è ancora disposto a giocare
      context.become(waitingReadyToJoinAnswerFromUser)
    }
  }


  //in attesa che l'utente risponda se è pronto
  def waitingReadyToJoinAnswerFromUser:Receive = UnexpectedShutdown orElse {
    case message: UserReadyToJoin => {
      println("--------------------------------------------------------------------")
      println(self + " L'utente " + sender() + " dice di essere pronto(" + message.ready + ")")
      println(self + " Inoltro risposta al greeting")
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
      println("ReadyToJoinAck arrivato")
      //valuto risposta del giocatore contenuta in playerIsReady
      playerIsReady match {
        case true => {
          //giocatore pronto per giocare
          println("l'utente ha joinato la partita")
          context.become(waitingGameServerTopic)
        }
        case false => {
          //giocatore non pronto alla partita
          println("l'utente non era pronto per joinare la partita")
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

  //attendo che il GameServer decida di chi è il turno //todo manca gestione arrivo messaggi in chat
  def waitingInTurnPlayerNomination: Receive =
    UnexpectedShutdown orElse
    opponentLefted orElse {
    case message: PlayerTurnBegins => {
      if (message.playerInTurn == self){
        //caso in cui spetta a me giocare
        println("ricevuto PlayerTurnBegins - è il mio turno: self=" + self + " === attore in turno = " + message.playerInTurn)
        Controller.userTurnBegins()
        context.become(waitingUserMakingMove)
      } else {
        // è il turno di un avversario, devo mettermi in attesa degli aggiornamenti di fine turno
        println("ricevuto PlayerTurnBegins - NON è il mio turno: self=" + self + " !== attore in turno = " + message.playerInTurn)
        context.become(waitingTurnEndUpdates)
      }
      sendPlayerInTurnAck() //invio ack al GameServer
    }
    case message: GameEnded => {
      println("--------------------------------------------------------------------")
      println(self + " Ricevuto messaggio di finepartita da GameServer; chiedo all'utente se vuole fare un altra partita; dico al GameServer di smettere di inviarmi messaggi GameEnded")
      sendGameEndedAck()
      Controller.matchEnded(message.name, message.actorRef==self)
      resetMatchInfo()
      context.become(waitingUserChoosingWheterPlayAgainOrClosing)
    }
  }



  //attendo che l'utente faccia la sua mossa per poi comunicarla al GameServer //todo manca gestione arrivo messaggi in chat
  def waitingUserMakingMove: Receive = UnexpectedShutdown orElse
    opponentLefted orElse {
    case message :UserMadeHisMove =>{
      println("--------------------------------------------------------------------")
      println("utente ha indicato la sua mossa: " + message.move)
      scheduler.replaceBehaviourAndStart(()=>sendUserMove(message.move))
      context.become(waitingMoveAckFromGameServer)
    }
  }

  //attendo che il server mi confermi la ricezione della mossa
  //todo manca gestione arrivo messaggi in chat
  def waitingMoveAckFromGameServer: Receive =  UnexpectedShutdown orElse
    opponentLefted orElse {
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


  //attendo che il GameServer comunichi gli aggiornamenti da compiere //todo manca gestione arrivo messaggi in chat
  def waitingTurnEndUpdates: Receive = UnexpectedShutdown orElse
    opponentLefted orElse {
    case message :EndTurnUpdate =>{
      println("ricevuti aggironamenti di fine turno dal GameServer [EndTurnUpdate]")
      Controller.turnEndUpdates(message.playersRanking, message.board)
      sendEndTurnUpdateAck()
      context.become(waitingInTurnPlayerNomination)
    }
    case msg => println("SONO IN waitingTurnEndUpdates => MESSAGGIO INATTESO: " + msg.toString())
  }


  //stato in cui attendo che il controller mi comunichi se l'utente vuole giocare una nuova partita o uscire //todo manca gestione arrivo messaggi in chat
  def waitingUserChoosingWheterPlayAgainOrClosing: Receive = UnexpectedShutdown orElse {
    case message: PlayAgain => {
      println("--------------------------------------------------------------------")
      println("utente dice che vuole continuare a giocare ("+message.userWantsToPlay+")")
      message.userWantsToPlay match {
        case true => {
          resetMatchInfo()
          //Controller.onLoginResponse()
          context.become(waitingUserQueueRequest)
        }
        case false => {
          println(self + " Invio richiesta di stop")
          scheduler.replaceBehaviourAndStart(()=>tearDownConnectionToGreetingServer())
          context.become(waitingDisconnectionAck)
        }
      }
    }
  }

  //attendo che GreetingServer confermi ricezione messaggio di disconnessione //todo manca gestione arrivo messaggi in chat
  def waitingDisconnectionAck: Receive = {
    case _: DisconnectionToGameServerNotificationAck => {
      println("--------------------------------------------------------------------")
      println(self + " - Ricevuto ack di richiesta disconnessione dal Game Server = " +sender())
      handleClientStop()
    }

    case _: DisconnectionAck => {
      println("--------------------------------------------------------------------")
      println(self + " - Ricevuto ack di richiesta disconnessione dal Greeting Server = " +sender())
      handleClientStop()
    }
  }


  //GESTIONE STOP CLIENT
  private def handleClientStop():Unit = {
    println(self + " Muoro felicio")
    scheduler.stopTask()
    //dovrò comunicare al controller la riuscita terminazione
    Controller.exit()
    context.stop(self)
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
    println(self + " - Ho inviato MatchTopicListenAck")
    gameServerActorRef.get ! MatchTopicListenAck()
  }

  //confermo a GameServer ricezione PlayerTurnBegins message
  private def sendPlayerInTurnAck(): Unit = {
    println(self + " - Ho inviato sendPlayerInTurnAck")
    gameServerActorRef.get ! PlayerTurnBeginAck()
  }


  //inoltro la mossa scelta dall'utente al GameServer
  private def sendUserMove(move:Move): Unit = {
    println(self + " - Ho inviato Usermove " + move+ "al GameServer")
    gameServerActorRef.get ! ClientMadeMove(move)
  }

  //confermo a GameServer ricezione EndTurnUpdate message
  private def sendEndTurnUpdateAck(): Unit = {
    println(self + " - Ho inviato EndTurnUpdateAck")
    gameServerActorRef.get ! EndTurnUpdateAck()
  }



  /*Il GameServer ha accettato la parola composta dall'utente, devo:
* - smettere di inviare la mossa fatta dall'utente al GameServer
* - comunicare la nuova mano dell'utente alla UI perchè venga visualizzata
* - passare allo stato in cui attendo update di fine turno
* */
  def onWordAccepted(hand:ArrayBuffer[Card]):Unit = {
    println("--------------------------------------------------------------------")
    println("ricevuto [WordAccepted] ack dal GameServer per ricezione mossa utente")
    Controller.moveOutcome(AcceptedWord(hand))
    context.become(waitingTurnEndUpdates)
  }

  /*Il GameServer NON ha accettato la parola composta dall'utente, devo:
  * - smettere di inviare la mossa fatta dall'utente al GameServer
  * - comunicare a UI il fallimento
  * - passare allo stato in cui una nuova mossa dall'utente
  * */
  def onWordRefused():Unit = {
    println("--------------------------------------------------------------------")
    println("ricevuto [WordRefused] ack dal GameServer per ricezione mossa utente")
    Controller.moveOutcome(RefusedWord())
    context.become(waitingUserMakingMove)
  }

  /*Il GameServer ha accettato la richiesta di sostituzione della mano fatta dall'utente, devo:
  * - smettere di inviare la mossa fatta dall'utente al GameServer
  * - comunicare la nuova mano dell'utente all'UI perchè venga visualizzata
  * - passare allo stato in cui attendo update di fine turno
  * */
  def onHandSwitchAccepted(hand:ArrayBuffer[Card]):Unit = {
    println("--------------------------------------------------------------------")
    println("ricevuto [HandSwitchAccepted] ack dal GameServer per ricezione mossa utente")
    Controller.moveOutcome(HandSwitchAccepted(hand))
    context.become(waitingTurnEndUpdates)
  }

  /*Il GameServer non ha accettato la richiesta di sostituzione della mano fatta dall'utente, devo:
  * - smettere di inviare la mossa fatta dall'utente al GameServer
  * - comunicare all'UI il fallimento
  * - passare allo stato in cui una nuova mossa dall'utente
  * */
  def onHandSwitchRefused():Unit = {
    println("--------------------------------------------------------------------")
    println("ricevuto [HandSwitchRefused] ack dal GameServer per ricezione mossa utente")
    Controller.moveOutcome(HandSwitchRefused())
    context.become(waitingUserMakingMove)
  }

  /*Il GameServer ha accettato la richiesta di passare il turno fatta dall'utente, devo:
  * - smettere di inviare la mossa fatta dall'utente al GameServer
  * - comunicare all'UI che tutto è andato a buon fine
  * - passare allo stato in cui attendo update di fine turno
  * */
  def onPassAck():Unit = {
    println("--------------------------------------------------------------------")
    println("ricevuto [onPassAck] ack dal GameServer per ricezione mossa utente")
    Controller.moveOutcome(PassReceived())
    context.become(waitingTurnEndUpdates)
  }

  /*Il GameServer ha ricevuto la notifica di timeout, devo:
  * - smettere di inviare la notifica fatta dall'utente al GameServer
  * - comunicare all'UI che tutto è andato a buon fine
  * - passare allo stato in cui attendo update di fine turno
  * */
  def onTimeoutAck():Unit = {
    println("--------------------------------------------------------------------")
    println("ricevuto [onTimeoutAck] ack dal GameServer per ricezione mossa utente")
    Controller.moveOutcome(TimeoutReceived())
    context.become(waitingTurnEndUpdates)
  }

  //comunico a GameServer ricezione del messaggio di terminazione della partita
  private def sendGameEndedAck(): Unit = {
    println(self + " - Ho inviato sendGameEndedAck")
    gameServerActorRef.get !  GameEndedAck()
  }










  //GESTIONE COLLOQUIO CON GREETING_SERVER

  //stabilisce la connessione con il greeting server
  private def estabilishConnectionToGreetingServer(): Unit = {
    println(self + " - Ho inviato ConnectionToGreetingQuery; name" +username.getOrElse("errore-username1"))
    mediator ! Publish(GREETING_SERVER_RECEIVES_TOPIC, ConnectionToGreetingQuery(username.getOrElse("default-name"))) //questo deve rimanere su topic
  }

  //utente conferma di voler partecipare alla partita o meno
  private def responseToJoinMatchRequest():Unit = {
    println(self + " - Ho inviato PlayerReadyAnswer: " + playerIsReady)
    greetingServerActorRef.get ! PlayerReadyAnswer(playerIsReady)
  }

  //invia al GreetingServer una notifica di disconnessione
  private def tearDownConnectionToGreetingServer(): Unit = {
    println(self + " - Ho inviato DisonnectionToGreetingNotification")
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
    println(self + " - Ho inviato SomeoneDisconnectedAck")
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

  //l'attore client termina se stesso //todo nota viene usato nei primi due stati finchè non vuoi cancellare quella print
  private def stopSelf(): Unit = {
    println("l'utente si è disconnesso, mi stoppo")
    context.stop(self)
  }

  //chiamato dopo che l'utente si è disconnesso inaspettatamente e non era in partita => comunico il fatto a GreetingServer
  def notifyDisconnectionToGreetingServer: Unit = {
    println("--------------------------------------------------------------------")
    println(self + " -Ricevuta richiesta di disconnessione dall'utente = " +sender())
    println(self + " Invio richiesta di stop al Greeting")
    scheduler.replaceBehaviourAndStart(()=>tearDownConnectionToGreetingServer())
  }

  //chiamato dopo che l'utente si è disconnesso inaspettatamente ed era in partita => comunico il fatto a GameServer
  def notifyDisconnectionToGameServer: Unit = {
    println("--------------------------------------------------------------------")
    println(self + " -Ricevuta richiesta di disconnessione dall'utente = " +sender())
    println(self + " Invio richiesta di stop al GameServer")
    scheduler.replaceBehaviourAndStart(()=>tearDownConnectionToGameServer())
  }


  //invio notifica di disconnessione al GameServer in seguito a UI chiusa forzatamente
  private def tearDownConnectionToGameServer(): Unit = {
    println(self + " - Ho inviato DisconnectionToGameServerNotification")
    gameServerActorRef.get ! DisconnectionToGameServerNotification()
  }


  //verifico se l'attore che è crollato è un server a me collegato
  private def handleServersUnexpectedShutdown(serverDown: ActorRef): Unit = {
    if(serverDown == greetingServerActorRef.get){
      handleGreetingServerDisconnection
    }else if (serverDown==gameServerActorRef.get){
      handleGameServerDisconnection
    }else {
      println("**************** !!!!\n\n\n HANNO UCCISO QUALCUNO CHE NON CONOSCO: " +serverDown+ " \n\n\n***********")
    }
  }

  //se crolla il greeting comunico alla UI e mi stoppo tanto non c'è piu niente da fare
  private def handleGreetingServerDisconnection: Unit = {
    println("**************** !!!!\n\n\n HANNO UCCISO GREETING_SERVER \n\n\n***********")
    //comunicare alla UI la morte del Greeting server, valutare se adottare un comportamento differente
    Controller.serversDown(GreetingServerDown())
    context.stop(self)
  }

  //gestisco il caso in cui crolla il GameServer
  private def handleGameServerDisconnection: Unit = {
    println("**************** !!!!\n\n\n HANNO UCCISO GAME_SERVER \n\n\n***********")
    if (greetingServerActorRef.nonEmpty) {
      //comunicare alla UI la morte del Game server, valutare se adottare un comportamento differente
      Controller.serversDown(GameServerDown())
      resetMatchInfo()
      //necessario saltare in uno stato in cui faccio scegliere a player che fare: se continuar a giocare o meno
      context.become(waitingUserQueueRequest)
    }
  }

  //resetta le variabili temporanee
  private def resetMatchInfo():Unit = {
    println("invocato metodo resetMatchInfo")
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
