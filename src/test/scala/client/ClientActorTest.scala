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
import ExtraMessagesForClientTesting._



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
           + Per ClientActor:
                private def  ClusterScheduler = new ClusterScheduler(6, TimeUnit.SECONDS, 6, TimeUnit.SECONDS, None,cluster)

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


  /*
  testo la capacità del Client gestire connessione al server:
    - client in grado di ricevere nome da utente
    - client in grado di ricevere stabilire connessione con GreetingServer
    - client notifica a utente che una partita è pronta quando richiesto dal GreetingServer

  */
  "Client"  should "be able to notify user when match is ready" in {
    //attore a cui il Controller invierà messaggi
    val controllerListener = TestProbe()
    //il mio client
    val client = system.actorOf(Props(new ClientToTest()), clientActorName+"2")
    //greetingServer
    val greetingServer = TestProbe()
    //avvia attore che ascolta il topic del greetingServer (GreetingServerTopicListener)
    system.actorOf(Props(new GreetingServerTopicListener(greetingServer.ref)), greetingSTopicListenerName+"2" )

    //l'utente effettua l'accesso e richiede di esser messo in coda per nuova partita
    testConnectionToGreetingServer(controllerListener, client, greetingServer)

    //Valuto la gestione di un messaggio che indica che il GreetingServer accetta la connessione
    greetingServer.send(client, ConnectionAnswer(true))
    //non dovrebbero esserci scambi di messaggi fino a quando Greeting non riuscirà a creare una partita
    bothReceivesNoMessageCheck(controllerListener, greetingServer)

    //GreetingServer riesce a creare una partita
    greetingServer.send(client, ReadyToJoinQuery())
    checkReceivedStringMessage(controllerListener, ASK_USER_TO_JOIN_GAME)
  }


  /*
  testo la capacità del Client gestire risposta negativa dell'utente alla richiesta di entrare in partita
    - il test parte da quando attendo richiesta utente pronto (waitingReadyToJoinRequestFromGreetingServer)
    - testo successivo scambio di messaggi con UI, GreetingServer e risposta a UI
 */
  "Client"  should "be able to handle user not ready for match" in {
    //attore a cui il Controller invierà messaggi
    val controllerListener = TestProbe()
    //il mio client
    val client = system.actorOf(Props(new ClientToTest()), clientActorName+"3")
    //greetingServer
    val greetingServer = TestProbe()
    //avvia attore che ascolta il topic del greetingServer (GreetingServerTopicListener)
    system.actorOf(Props(new GreetingServerTopicListener(greetingServer.ref)), greetingSTopicListenerName+"3" )

    //inizializzo il controller
    controllerInitCheck(controllerListener, client)

    //trick per skippare parti già testate ed andare dritto a WaitingReadyToJoinRequestFromGreetingServer
    client ! JumpToWaitingReadyToJoinRequestFromGreetingServer(greetingServer.ref, username)
    //controlla che sia avvenuto il setup
    expectMsgType[SetUpDoneWRTJRFGS](new FiniteDuration(waitTimeForMessages,TimeUnit.SECONDS))

    //greeting server è riuscito a creare una partita vorrebbe che l'utente joinasse
    greetingServer.send(client, ReadyToJoinQuery())
    //richiesta all'utente se è disponibile per la partita
    checkReceivedStringMessage(controllerListener, ASK_USER_TO_JOIN_GAME)

    //l'utente non è disponibile a entrare in partita
    client ! UserReadyToJoin(false)
    //client dovrebbe comunicare a GreetingServer tale decisione del player
    val state1 = greetingServer.expectMsgType[PlayerReadyAnswer](new FiniteDuration(waitTimeForMessages,TimeUnit.SECONDS))
    state1 must equal(PlayerReadyAnswer(false))

    //GreetingServer invia Ack al client
    greetingServer.send(client, ReadyToJoinAck())

    //in questo stato greetingServer non dovrebbe ricevere messaggi
    actorReceivesNoMessageCheck(greetingServer)

    //client dovrebbe far aggiornare la UI
    val state2 = controllerListener.expectMsgType[String](new FiniteDuration(waitTimeForMessages,TimeUnit.SECONDS))
    state2 must equal(ON_LOGIN_RESPONSE)

    //testo che utente possa effettuare richiesta per nuova partita
    client ! JoinQueue()
    greetingServer.expectMsgType[ConnectionToGreetingQuery](new FiniteDuration(waitTimeForMessages,TimeUnit.SECONDS))
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


  //spesso mi troverò a controllare che nessun altro attore oltre a Client riceva messaggi
  private def bothReceivesNoMessageCheck(actor1: TestProbe, actor2: TestProbe): Unit = {
    actorReceivesNoMessageCheck(actor1)
    actorReceivesNoMessageCheck(actor2)
  }

  //si assicura che l'attore indicato non riceva messaggi (per un certo dt)
  private def actorReceivesNoMessageCheck(actor: TestProbe):Unit = actor.expectNoMessage(new FiniteDuration(secondsWithoutMessages,TimeUnit.SECONDS))

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
