package client

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import shared.ClientToGreetingMessages._
import shared.{ClusterScheduler, CustomScheduler}
import shared.Topic.GREETING_SERVER_RECEIVES_TOPIC
import shared.GreetingToClientMessages._

class ClientActor extends Actor{
  private val mediator = DistributedPubSub.get(context.system).mediator
  private val cluster = Cluster.get(context.system)
  private val scheduler: CustomScheduler = ClusterScheduler(cluster)

  //contiene l'ActorRef del server
  private var greetingServerActorRef: Option[ActorRef] = None
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
      //todo gestire fasi successive sia che sia positivo che negativo la risposta del client
    }
  }


  def askUserToJoinGame():Unit = {
    //todo girare domanda all'utente attraverso UI: l'utente è pronto a giocare?
  }

  //comunico alla UI il fatto che il GreetingServer non permetta di stabilire una connessione
  private def handleConnectionFailed(): Unit = {
    //todo comunicare al player che la connessione non può essere stabilita e chiudere
  }

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

  //controllo se l'utente ha effettuato uno shutdown forzato dell'applicazione
  def UnexpectedShutdown: Receive = {
    //todo aggiungere controllo su crollo lato UI
    case deathMessage:Terminated => handleServerUnexpectedShutdown(deathMessage.actor)
  }

  //verifico se l'attore che è crollato è un server a me collegato
  def handleServerUnexpectedShutdown(serverDown: ActorRef): Unit = {
    //todo se verrà implementato un server di gioco diverso dal greeting come progettato, dovrai controllare che l'attore morto non sia quel server
    if (serverDown == greetingServerActorRef.get){
      handleGreetingServerDisconnection
    } else {
      println("**************** !!!!\n\n\n HANNO UCCISO QUALCUNO CHE NON CONOSCO: " +serverDown+ " \n\n\n***********")
    }
  }

  //se crolla il greeting comunico alla UI e mi stoppo tanto non c'è piu niente da fare
  def handleGreetingServerDisconnection: Unit = {
    println("**************** !!!!\n\n\n HANNO UCCISO GREETING_SERVER \n\n\n***********")
    //todo comunicare alla UI la morte del Greeting server, valutare se adottare un comportamento differente
    context.stop(self)
  }
}

object ClientActor{
  def props() = Props(classOf[ClientActor])
}
