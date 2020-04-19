package client

import akka.actor.ActorRef
import client.ExtraMessagesForClientTesting._
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe

sealed trait ExtraMessagesForClientTestingType
object ExtraMessagesForClientTesting{
  //mi permette di saltare dritto allo stato WaitingReadyToJoinRequestFromGreetingServer, settando gli opportuni parametri che avrei dovuto normalmente ottenere
  case class JumpToWaitingReadyToJoinRequestFromGreetingServer(greetingServer:ActorRef, username:String) extends ExtraMessagesForClientTestingType

  //messaggio inviato in risposta ai messaggi parametri ricevuti in jumpToWRTJRFGS, log contiene descrizione stampabile di cosa stia avvenendo
  case class SetUpDoneWRTJRFGS(log: String = "") extends ExtraMessagesForClientTestingType
}

/*
  - ho creato questa estensione dell'attore per poter aggiungere dei salti a degli stati controllati;
    per quanto sia poco ortodosso forse, permette di snellire la fase di test e andare con ogni test a valutare
    solo gli aspetti fondamentali per una specifica funzionalità. Usare questa classe mi permette inoltre di non
    apportare modifiche utili solo ai fini di test alla classe attore utilizzata nel normale funzionamento
 */
class ClientToTest extends ClientActor {
  //setta le variabili contenenti ActorRef di GreetingServer e nome dell'utente, permettendo di saltare stati di inizializzazione
  private def setGreetingConnectionVariables(greetingServer:ActorRef, username:String): Unit ={
    this.username = Some(username)
    greetingServerActorRef = Some(greetingServer)
  }

  //permette di saltare nello stato WaitingReadyToJoinRequestFromGreetingServer
  def jumpToWAOUQR: Receive = {
    case msg: JumpToWaitingReadyToJoinRequestFromGreetingServer => {
      setGreetingConnectionVariables(msg.greetingServer, msg.username)
      sender ! SetUpDoneWRTJRFGS()
      context.become(waitingReadyToJoinRequestFromGreetingServer)
    }
  }

  //faccio si che dal primo stato io possa saltare in altri stati fondamentali, rendendo piu agile il test dell'attore
  override def waitingUsernameFromUser: Receive =
    jumpToWAOUQR orElse
      super.waitingUsernameFromUser
}