package client

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, Unsubscribe}
import model.Card
import shared.ClientToGameServerMessages.{ClientMadeMove, EndTurnUpdateAck, MatchTopicListenAck, PlayerTurnBeginAck}
import shared.ClientToGreetingMessages._
import shared.GameServerToClientMessages.{EndTurnUpdate, MatchTopicListenQuery, PlayerTurnBegins}
import shared.{ClusterScheduler, CustomScheduler, Move}
import shared.Topic.GREETING_SERVER_RECEIVES_TOPIC
import shared.GreetingToClientMessages._
import shared.Move.FakeMove

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
  //contiene lo username scelto dall'utente
  private var username: Option[String] = None
  //l'utente è disposto a giocare
  private var playerIsReady:Boolean = true //todo andrà settato a false quando interazione con UI sarà pronta

  mediator ! Subscribe(GREETING_SERVER_RECEIVES_TOPIC, self)

  override def receive: Receive = waitingRequestForGameModeFromGreetingServer

  //todo: dovrei attendere che l'utente mi passi il nome, per ora lo genero io e parto dalla fase dell'invio
  scheduler.replaceBehaviourAndStart(()=>estabilishConnectionToGreetingServer())

  //attendo che il Greeting mi richieda il GameMode
  def waitingRequestForGameModeFromGreetingServer: Receive = UnexpectedShutdown orElse {
    case connection: ConnectionAnswer => connection.connected match {
      case true =>{
        scheduler.stopTask()
        greetingServerActorRef = Some(sender)
        context.watch(sender) //mi metto in ascolto di una eventuale morte del GreetingServer
        println("Client " + self + " ricevuto ConnectionAnswer positiva ["+ connection.connected +"] dal GreetingServer "+ greetingServerActorRef.get)
        context.become(waitingReadyToJoinRequestFromGreetingServer)
      }
      case false => {
        scheduler.stopTask()
        handleConnectionFailed()
        println("Client " + self + " ricevuto ConnectionAnswer negativa ["+ connection.connected +"] dal GreetingServer "+ greetingServerActorRef.get)
      }
    }
  }

  //attendo richiesta di join partita da parte del greeting server
  def waitingReadyToJoinRequestFromGreetingServer: Receive = UnexpectedShutdown orElse {
    case _: ReadyToJoinQuery => {
      println("--------------------------------------------------------------------")
      println(self + " - Ricevuto richiesta di join match dal GreetingServer = " +sender())
      askUserToJoinGame() //chiedo all'UI di chiedere all'utente se è ancora disposto a giocare

      /*
        todo in teoria dovrei saltare in uno stato in cui attendo la risposta dell'UI
         ma per ora salto questo passaggio: rispondo direttamente si e salto
         nello stato di attesa dell'ack da parte del GreetingServer
       */
      scheduler.replaceBehaviourAndStart(()=>responseToJoinMatchRequest())
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
          //todo devi portare il giocatore in uno stato di attesa ancora non implementato
        }
      }
    }
  }

  //attendo che il GameServer mi invii il messaggio con le informazioni per impostare la partita lato client
  def waitingGameServerTopic: Receive = UnexpectedShutdown orElse {
    case topicMessage: MatchTopicListenQuery =>

      updateGameServerReference(sender())
      updateGameServerTopic(topicMessage.gameServerTopic)
      //todo forse in questo momento vorresti ricevere e gestire tutte info da mostrare a giocatore in partita tra cui lista dei giocatori e chat
      //todo comunicare al player la propria mano attuale attraverso la UI
      sendGameServerTopicReceived()
      context.become(waitingInTurnPlayerNomination)
  }

  //attendo che il GameServer decida di chi è il turno //todo manca gestione arrivo messaggi in chat
  def waitingInTurnPlayerNomination: Receive = {
    UnexpectedShutdown orElse{
      //todo in questo momento mi aspetto eventualmente un messaggio di terminazione della partita
      case message: PlayerTurnBegins => {
        message.playerInTurn match {
          case self => {
            //caso in cui spetta a me giocare
            println("ricevuto PlayerTurnBegins - è il mio turno: self=" + self + " === attore in turno = " + message.playerInTurn)
            /*
                todo devi notificare all'UI l'inizio del turno del player ed in teoria attendere una mossa del player,
                 ma per ora la implemento direttamente e salto la fase in cui attendo mossa dall'UI
             */
            scheduler.replaceBehaviourAndStart(()=>sendUserMove(FakeMove())) //todo da eliminare
            context.become(waitingMoveAckFromGameServer) //todo questo salto dovrà andare verso uno stato intermedio di attesa mossa da parte di UI
          }
          case _ => {
            // è il turno di un avversario, devo mettermi in attesa degli aggiornamenti di fine turno
            println("ricevuto PlayerTurnBegins - NON è il mio turno: self=" + self + " !== attore in turno = " + message.playerInTurn)
            context.become(waitingTurnEndUpdates)
          }
        }
        sendPlayerInTurnAck() //invio ack al GameServer
      }
    }
  }

  //attendo che il server mi confermi la ricezione della mossa //todo ricorda di smettere di inviare la mossa scelta una volta ricevuto ack
  def waitingMoveAckFromGameServer: Receive = ???


  //attendo che il GameServer comunichi gli aggiornamenti da compiere //todo manca gestione arrivo messaggi in chat
  def waitingTurnEndUpdates: Receive = UnexpectedShutdown orElse {
    case endTurnUpdateMessage :EndTurnUpdate =>{
      println("ricevuti aggironamenti di fine turno dal GameServer [EndTurnUpdate]")
      //todo dovrò probabilmente aggiornare UI
      sendEndTurnUpdateAck()
      context.become(waitingInTurnPlayerNomination)
    }
  }
















  //GESTIONE MESSAGGI GAME_SERVER


  //memorizza l'actorRef del GameServer e si registra per esser informato di un suo crollo
  private def updateGameServerReference(gameServer: ActorRef): Unit ={
    gameServerActorRef = Some(gameServer)
    context.watch(gameServer)  //todo a fine partita devo smettere di ascoltarlo
  }

  //memorizza il topic relativo al GameServer e si registra per esser informato di un suo crollo
  private def updateGameServerTopic(topic: String): Unit ={
    gameServerTopic = Some(topic)
    mediator ! Subscribe(gameServerTopic.get, self)  //todo a fine partita devo disiscrivermi
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








  // GESTIONE RAPPORTO CON UI

  //richiedo all'UI che l'utente dica se è pronto o meno per partecipare a una partita
  private def askUserToJoinGame():Unit = {
    //todo girare domanda all'utente attraverso UI: l'utente è pronto a giocare?
  }

  //comunico alla UI il fatto che il GreetingServer non permetta di stabilire una connessione
  private def handleConnectionFailed(): Unit = {
    //todo comunicare al player che la connessione non può essere stabilita e chiudere
  }








  //GESTIONE COLLOQUIO CON GREETING_SERVER

  //runnable sending connection request to GreetingServer
  private def estabilishConnectionToGreetingServer(): Unit = {
    println(self + " - Ho inviato ConnectionToGreetingQuery; name" +username.getOrElse("errore-username1"))
    mediator ! Publish(GREETING_SERVER_RECEIVES_TOPIC, ConnectionToGreetingQuery(username.getOrElse("default-name"))) //questo deve rimanere su topic
  }

  //client tells GreetingServer whether he wants to join match
  private def responseToJoinMatchRequest():Unit = {
    println(self + " - Ho inviato PlayerReadyAnswer: " + playerIsReady)
    greetingServerActorRef.get ! PlayerReadyAnswer(playerIsReady)
  }













  //GESTIONE DELLA DISCONNESSIONE DAI SERVER

  //controllo se l'utente ha effettuato uno shutdown forzato dell'applicazione
  private def UnexpectedShutdown: Receive = {
    //todo aggiungere controllo su crollo lato UI
    case deathMessage:Terminated => handleServersUnexpectedShutdown(deathMessage.actor)
  }

  //verifico se l'attore che è crollato è un server a me collegato
  private def handleServersUnexpectedShutdown(serverDown: ActorRef): Unit = {
    serverDown match {
      case greetingServerActorRef.get => handleGreetingServerDisconnection
      case gameServerActorRef.get => handleGameServerDisconnection
      case _ => println("**************** !!!!\n\n\n HANNO UCCISO QUALCUNO CHE NON CONOSCO: " +serverDown+ " \n\n\n***********")
    }
  }

  //se crolla il greeting comunico alla UI e mi stoppo tanto non c'è piu niente da fare
  private def handleGreetingServerDisconnection: Unit = {
    println("**************** !!!!\n\n\n HANNO UCCISO GREETING_SERVER \n\n\n***********")
    //todo comunicare alla UI la morte del Greeting server, valutare se adottare un comportamento differente
    context.stop(self)
  }

  //gestisco il caso in cui crolla il GameServer
  private def handleGameServerDisconnection: Unit = {
    println("**************** !!!!\n\n\n HANNO UCCISO GAME_SERVER \n\n\n***********")
    if (greetingServerActorRef.nonEmpty) {
      //todo comunicare alla UI la morte del Greeting server, valutare se adottare un comportamento differente
      resetMatchInfo()
      //todo necessario saltare in uno stato in cui faccio scegliere a player che fare: se continuar a giocare o meno
    }
  }

  //resetta le variabili temporanee
  def resetMatchInfo():Unit = {
    println("invocato metodo resetMatchInfo")
    if(gameServerActorRef.isDefined){
      context.unwatch(gameServerActorRef.get)
    }
    if (gameServerTopic.isDefined){
      mediator ! Unsubscribe(gameServerTopic.get, self)
    }
    gameServerTopic = None
    gameServerActorRef = None
    playerIsReady = false //todo occhio se faccio test finchè non c'è comunicazione con UI questo andrebbe messo a true
  }
}

object ClientActor{
  def props() = Props(classOf[ClientActor])
}
