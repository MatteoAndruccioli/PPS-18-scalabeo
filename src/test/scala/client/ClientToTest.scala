package client

import akka.actor.ActorRef
import client.ExtraMessagesForClientTesting._
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import client.controller.Controller


//contiene messaggi utilizzati in fase di test dall'attore ClientToTest
sealed trait ExtraMessagesForClientTestingType
object ExtraMessagesForClientTesting{
  //mi permette di saltare allo stato WaitingReadyToJoinRequestFromGreetingServer, settando gli opportuni parametri che avrei dovuto normalmente ottenere
  case class JumpToWaitingReadyToJoinRequestFromGreetingServer(greetingServer:ActorRef, username:String) extends ExtraMessagesForClientTestingType
  //messaggio inviato in risposta a JumpToWaitingReadyToJoinRequestFromGreetingServer, log contiene descrizione stampabile di cosa stia avvenendo
  case class SetUpDoneWRTJRFGS(log: String = "") extends ExtraMessagesForClientTestingType

  //mi permette di saltare allo stato WaitingGameServerTopic, settando gli opportuni parametri che avrei dovuto normalmente ottenere
  case class JumpToWaitingGameServerTopic(greetingServer:ActorRef, username:String) extends ExtraMessagesForClientTestingType
  //messaggio inviato in risposta a JumpToWaitingGameServerTopic, log contiene descrizione stampabile di cosa stia avvenendo
  case class SetUpDoneWGST(log: String = "") extends ExtraMessagesForClientTestingType

  //mi permette di saltare allo stato WaitingUserChoosingWheterPlayAgainOrClosing, settando gli opportuni parametri che avrei dovuto normalmente ottenere
  case class JumpToWaitingUserChoosingWheterPlayAgainOrClosing(greetingServer:ActorRef, username:String, gameServer:ActorRef, gameServerTopic:String) extends ExtraMessagesForClientTestingType
  //messaggio inviato in risposta a JumpToWaitingUserChoosingWheterPlayAgainOrClosing, log contiene descrizione stampabile di cosa stia avvenendo
  case class SetUpDoneWUCWPAOC(log: String = "") extends ExtraMessagesForClientTestingType

  //mi permette di saltare allo stato WaitingInTurnPlayerNomination, settando gli opportuni parametri che avrei dovuto normalmente ottenere
  case class JumpToWaitingInTurnPlayerNomination(greetingServer:ActorRef, username:String, gameServer:ActorRef, gameServerTopic:String) extends ExtraMessagesForClientTestingType
  //messaggio inviato in risposta a JumpToWaitingInTurnPlayerNomination, log contiene descrizione stampabile di cosa stia avvenendo
  case class SetUpDoneWITPN(log: String = "") extends ExtraMessagesForClientTestingType
}

/*
    ho creato questa estensione dell'attore per poter aggiungere dei salti a degli stati controllati;
    questo permette di snellire la fase di test e andare con ogni test a valutare
    solo gli aspetti fondamentali per una specifica funzionalità. Usare questa classe mi permette inoltre di non
    apportare modifiche utili solo ai fini di test alla classe attore utilizzata nel normale funzionamento
 */
class ClientToTest extends ClientActor {
  //setta le variabili contenenti ActorRef di GreetingServer e nome dell'utente, permettendo di saltare stati di inizializzazione
  private def setGreetingConnectionVariables(greetingServer:ActorRef, username:String): Unit ={
    this.username = Some(username)
    greetingServerActorRef = Some(greetingServer)
  }

  //oltre alle variabili di inizializzazione indica che l'utente è pronto a giocare
  private def setReadyPlayerVariables(greetingServer:ActorRef, username:String):Unit = {
    setGreetingConnectionVariables(greetingServer, username)
    playerIsReady = true
  }

  //setta tutte le variabili che vengono impostate da avvio all'inizio della fase di gioco, permettendo di testare solo la fase di gioco vera e propria
  private def setUpGameVariables(greetingServer:ActorRef, username:String, gameServer:ActorRef, gameServerTopic:String):Unit = {
    setReadyPlayerVariables(greetingServer, username)
    gameServerActorRef = Some(gameServer)
    this.gameServerTopic = Some(gameServerTopic)
    mediator ! Subscribe(this.gameServerTopic.get, self)
  }

  //permette di saltare nello stato WaitingReadyToJoinRequestFromGreetingServer
  def jumpToWAOUQR: Receive = {
    case msg: JumpToWaitingReadyToJoinRequestFromGreetingServer =>
      setGreetingConnectionVariables(msg.greetingServer, msg.username)
      sender ! SetUpDoneWRTJRFGS()
      context.become(waitingReadyToJoinRequestFromGreetingServer)
  }

  //permette di saltare nello stato WaitingGameServerTopic
  def jumpToWGST: Receive = {
    case msg: JumpToWaitingGameServerTopic =>
      setReadyPlayerVariables(msg.greetingServer, msg.username)
      sender ! SetUpDoneWGST()
      context.become(waitingGameServerTopic)
  }



  //permette di saltare nello stato waitingUserChoosingWheterPlayAgainOrClosing
  def jumpToWUCWPAOC: Receive = {
    case msg: JumpToWaitingUserChoosingWheterPlayAgainOrClosing =>
      setUpGameVariables(msg.greetingServer, msg.username, msg.gameServer, msg.gameServerTopic)
      resetMatchInfo()
      sender ! SetUpDoneWUCWPAOC()
      context.become(waitingUserChoosingWheterPlayAgainOrClosing)
  }

  //permette di saltare nello stato waitingInTurnPlayerNomination
  def jumpToWITPN: Receive = {
    case msg: JumpToWaitingInTurnPlayerNomination =>
      setUpGameVariables(msg.greetingServer, msg.username, msg.gameServer, msg.gameServerTopic)
      sender ! SetUpDoneWITPN()
      context.become(waitingInTurnPlayerNomination)
  }

  //gestione chiusura gioco, elimino lo stop dell'attore per evitare chiusura ActorSystem nel test di fine partita
  override def handleClientStop():Unit = {
    scheduler.stopTask()
    Controller.exit()
  }


  //faccio si che dal primo stato io possa saltare in altri stati fondamentali, rendendo piu agile il test dell'attore
  override def waitingUsernameFromUser: Receive =
    jumpToWAOUQR orElse
    jumpToWGST orElse
    jumpToWUCWPAOC orElse
    jumpToWITPN orElse
      super.waitingUsernameFromUser
}