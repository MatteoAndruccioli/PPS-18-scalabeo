package client

import java.util.concurrent.TimeUnit
import client.controller.Messages.ViewToClientMessages._
import shared.ClientToGreetingMessages._
import shared.GreetingToClientMessages._
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import client.controller.Controller
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.must.Matchers
import scala.concurrent.duration.FiniteDuration

import TestOnClientCostants._
import ClientTestConstants._



object TestOnClientCostants{
  val TEST_SYSTEM_NAME = "test-system"
  val username :String= "UsernameTest"
  val clientActorName: String = "Client"
  val greetingSTopicListenerName: String = "greetingTopicListener" //greeting server topic listener

  val chatTopic: String = "chatTopic" //game server topic sender
  /*
    nota: dal momento che lavoriamo con uno scheduler, i valori di tempo influiscono sia sulla correttezza dei
         test sia sull'efficacia ed efficienza del programma; in particolare i test sono stati svolti cercando di determinare
         i migliori valori per vari parametri temporali, quelli da utilizzare vengono riportati in seguito:
           + IN QUESTA CLASSE:
                val waitTimeForMessages: Int = 10
                val secondsWithoutMessages: Int = 10
           + IN Client6:
                private def istantiateEmptyScheduler(): ClusterScheduler = new ClusterScheduler(6, TimeUnit.SECONDS, 6, TimeUnit.SECONDS, None,cluster)

   */
  val waitTimeForMessages: Int = 10
  val secondsWithoutMessages: Int = 10
}

class ClientActorTest extends TestKit (ActorSystem(TEST_SYSTEM_NAME))
  with AnyFlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers {

  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }


  /*
  testo la capacità del Client gestire connessione al server:
    - client in grado di ricevere nome da utente
    - client in grado di ricevere richiesta connessione da utente
    - client in grado di gestire connessione impossibile da stabilire
 */
  "Client"  should "be able to send connection request to GreetingServer" in {
    //attore a cui il Controller invierà messaggi
    val controllerListener = TestProbe()
    //il mio client
    val client = system.actorOf(Props(new ClientToTest()), clientActorName)
    //greetingServer
    val greetingServer = TestProbe()
    //avvia attore che ascolta il topic del greetingServer (GreetingServerTopicListener)
    system.actorOf(Props(new GreetingServerTopicListener(greetingServer.ref)), greetingSTopicListenerName+"1" )

    //l'utente effettua l'accesso e richiede di esser messo in coda per nuova partita
    testConnectionToGreetingServer(controllerListener, client, greetingServer)

    //Valuto la gestione di un messaggio che indica che il GreetingServer rifiuta la connessione
    greetingServer.send(client, ConnectionAnswer(false))
    checkReceivedStringMessage(controllerListener, ON_CONNECTION_FAILED)
  }


  //--------------------METODI TEST CHIAMATI PIU VOLTE-------------------


  /*
     Simula interazione UI-Client-GreetingServer da avvio a richiesta di connessione
      - inizializza controller
      - utente comunica username e viene effettuato relativo check
      - utente richiede di essere messo in coda per nuova partita -> verifica che greetingServer riceva richiesta
   */
  private def testConnectionToGreetingServer(controllerListener: TestProbe,
                                             client: ActorRef,
                                             greetingServer: TestProbe):Unit = {
    //alla chiamata init deve essere avviata la GUI
    controllerInitCheck(controllerListener,client)

    //dopo aver ricevuto il nome scelto dall'utente deve essere invocata onLoginResponse
    client ! UsernameChosen(username)
    checkReceivedStringMessage(controllerListener, ON_LOGIN_RESPONSE)

    //UI richiede di essere messo in coda per nuova partita
    client ! JoinQueue()
    greetingServer.expectMsgType[ConnectionToGreetingQuery](new FiniteDuration(waitTimeForMessages,TimeUnit.SECONDS))
  }

  //---------------------------------------METODI DI UTILITY------------------------------


  //inizializza il controller ed effettua check su di esso
  private def controllerInitCheck(controllerListener: TestProbe,
                                  client: ActorRef): Unit = {
    Controller.init(client, TestMind(false, controllerListener.ref))
    checkReceivedStringMessage(controllerListener, START_GUI)
  }

  //controlla che receiver riceva il expectedMessage entro limiti di tempo prestabiliti
  private def checkReceivedStringMessage(receiver: TestProbe, expectedMessage:String):Unit = {
    var state = receiver.expectMsgType[String](new FiniteDuration(waitTimeForMessages,TimeUnit.SECONDS))
    state must equal(expectedMessage)
    println("checkReceivedStrinMessage: " + state  + " == " + expectedMessage)
  }

}
