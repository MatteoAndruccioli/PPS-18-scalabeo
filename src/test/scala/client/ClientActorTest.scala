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
import client.ClientTestMessage.{MatchEnded, OnMatchStart, TurnEndUpdates}
import client.controller.MoveOutcome.AcceptedWord
import model.{BoardTile, BoardTileImpl, Card, CardImpl, PositionImpl}
import shared.ClientMoveAckType.WordAccepted
import shared.ClientToGameServerMessages._
import shared.GameServerToClientMessages._
import shared.Move.WordMove




object TestOnClientCostants{
  val TEST_SYSTEM_NAME = "test-system" //nome dell'ActorSystem
  val USERNAME :String= "UsernameTest" //nome dell'utente
  val CLIENTACTOR_NAME: String = "Client" //nome dell'attore Client
  val GREETING_TOPIC_LISTENER_NAME: String = "greetingTopicListener" //greeting server topic listener
  val GAME_TOPIC_SENDER_NAME: String = "gameTopicSender" //game server topic sender
  val GAME_SERVER_TOPIC: String = "GameServerTopic" //game server topic sender
  /*
    nota: dal momento che lavoriamo con uno scheduler, i valori di tempo influiscono sia sulla correttezza dei
         test sia sull'efficacia ed efficienza del programma; in particolare i test sono stati svolti cercando di determinare
         i migliori valori per vari parametri temporali, quelli da utilizzare vengono riportati in seguito:
           + IN QUESTA CLASSE:
                val waitTimeForMessages: Int = 10
                val secondsWithoutMessages: Int = 10
           + Valori per ClusterScheduler usato in ClientActor:
                  val INITIAL_DELAY :Long = 6;
                  val INTERVAL :Long = 6;
   */
  val WAIT_TIME_MESSAGES: Int = 10  //dt (in secondi) entro cui un certo messaggio dovrebbe arrivare
  val TIME_WITHOUT_MESSAGES: Int = 10 //dt (in secondi) in cui valuto se un attore riceve messaggi
  val DEFAULT_CARD: CardImpl = CardImpl("NULL") //implementazione dummy di una carta
}

class ClientActorTest extends TestKit (ActorSystem(TEST_SYSTEM_NAME))
  with AnyFlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers {

  override def afterAll:Unit = {
    TestKit.shutdownActorSystem(system)
  }


  /*
  testo la capacità del Client gestire connessione al server:
    - client in grado di ricevere nome da utente
    - client in grado di ricevere richiesta connessione da utente
    - client in grado di gestire connessione impossibile da stabilire
 */
  "Client"  should "be able to send connection request to GreetingServer" in {
    val testId: String = "1"
    //attore a cui il Controller invierà messaggi
    val controllerListener = TestProbe()
    //il mio client
    val client = system.actorOf(Props(new ClientToTest()), CLIENTACTOR_NAME)
    //greetingServer
    val greetingServer = TestProbe()
    //avvia attore che ascolta il topic del greetingServer (GreetingServerTopicListener)
    system.actorOf(Props(new GreetingServerTopicListener(greetingServer.ref)), GREETING_TOPIC_LISTENER_NAME+testId )

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
    val testId: String = "2"
    //attore a cui il Controller invierà messaggi
    val controllerListener = TestProbe()
    //il mio client
    val client = system.actorOf(Props(new ClientToTest()), CLIENTACTOR_NAME+testId)
    //greetingServer
    val greetingServer = TestProbe()
    //avvia attore che ascolta il topic del greetingServer (GreetingServerTopicListener)
    system.actorOf(Props(new GreetingServerTopicListener(greetingServer.ref)), GREETING_TOPIC_LISTENER_NAME+testId )

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
    val testId: String = "3"
    //attore a cui il Controller invierà messaggi
    val controllerListener = TestProbe()
    //il mio client
    val client = system.actorOf(Props(new ClientToTest()), CLIENTACTOR_NAME+testId)
    //greetingServer
    val greetingServer = TestProbe()
    //avvia attore che ascolta il topic del greetingServer (GreetingServerTopicListener)
    system.actorOf(Props(new GreetingServerTopicListener(greetingServer.ref)), GREETING_TOPIC_LISTENER_NAME+testId )

    //inizializzo il controller
    controllerInitCheck(controllerListener, client)

    //trick per skippare parti già testate ed andare dritto a WaitingReadyToJoinRequestFromGreetingServer
    client ! JumpToWaitingReadyToJoinRequestFromGreetingServer(greetingServer.ref, USERNAME)
    //controlla che sia avvenuto il setup
    expectMsgType[SetUpDoneWRTJRFGS](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))

    //greeting server è riuscito a creare una partita vorrebbe che l'utente joinasse
    greetingServer.send(client, ReadyToJoinQuery())
    //richiesta all'utente se è disponibile per la partita
    checkReceivedStringMessage(controllerListener, ASK_USER_TO_JOIN_GAME)

    //l'utente non è disponibile a entrare in partita
    client ! UserReadyToJoin(false)
    //client dovrebbe comunicare a GreetingServer tale decisione del player
    val state1 = greetingServer.expectMsgType[PlayerReadyAnswer](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))
    state1 must equal(PlayerReadyAnswer(false))

    //GreetingServer invia Ack al client
    greetingServer.send(client, ReadyToJoinAck())

    //in questo stato greetingServer non dovrebbe ricevere messaggi
    actorReceivesNoMessageCheck(greetingServer)

    //client dovrebbe far aggiornare la UI
    val state2 = controllerListener.expectMsgType[String](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))
    state2 must equal(ON_LOGIN_RESPONSE)

    //dovrebbe essere possibile contattare il GreetingServer
    testNewGameRequest(client, greetingServer)
  }



  /*
  testo la capacità del Client gestire risposta positiva dell'utente alla richiesta di entrare in partita
    - il test parte da quando attendo richiesta utente pronto (waitingReadyToJoinRequestFromGreetingServer)
    - testo successivo scambio di messaggi con UI, GreetingServer e risposta a UI
    - l'ultimo passo consiste nella gestione del primo messaggio proveniente dal GameServer
  */
  "Client"  should "be able to handle game start" in {
    val testId: String = "4"
    //attore a cui il Controller invierà messaggi
    val controllerListener = TestProbe()
    //il mio client
    val client = system.actorOf(Props(new ClientToTest()), CLIENTACTOR_NAME+testId)
    //greetingServer
    val greetingServer = TestProbe()
    //avvia attore che ascolta il topic del greetingServer (GreetingServerTopicListener)
    system.actorOf(Props(new GreetingServerTopicListener(greetingServer.ref)), GREETING_TOPIC_LISTENER_NAME+testId )
    //GameServer
    val gameServer = TestProbe()
    //topic del GameServer
    val gsTopic :String= GAME_SERVER_TOPIC+testId
    //attore che invia i messaggi sul topic del gameserver
    val gameServerTopicSender = system.actorOf(Props(new OnGameTopicSenderActor(gsTopic, gameServer.ref)), GAME_TOPIC_SENDER_NAME+"4")

    //inizializzo il controller
    controllerInitCheck(controllerListener, client)

    //trick per skippare parti già testate ed andare dritto a WaitingReadyToJoinRequestFromGreetingServer
    client ! JumpToWaitingReadyToJoinRequestFromGreetingServer(greetingServer.ref, USERNAME)
    //controlla che sia avvenuto il setup
    expectMsgType[SetUpDoneWRTJRFGS](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))

    //greeting server è riuscito a creare una partita vorrebbe che l'utente joinasse
    greetingServer.send(client, ReadyToJoinQuery())
    //richiesta all'utente se è disponibile per la partita
    checkReceivedStringMessage(controllerListener, ASK_USER_TO_JOIN_GAME)

    //l'utente non è disponibile a entrare in partita
    client ! UserReadyToJoin(true)
    //client dovrebbe comunicare a GreetingServer tale decisione del player
    val state1 = greetingServer.expectMsgType[PlayerReadyAnswer](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))
    state1 must equal(PlayerReadyAnswer(true))

    //GreetingServer invia Ack al client
    greetingServer.send(client, ReadyToJoinAck())

    //in questo stato greetingServer non dovrebbe ricevere messaggi
    actorReceivesNoMessageCheck(greetingServer)

    //testo interazione di inizio partita
    testConnectionToGameServer(controllerListener, client, gameServer, gsTopic)
  }

  /*
  testo la capacità del Client gestire terminazione della partita
    - il test parte da quando attendo primo messaggio GameServer -> lo gestisco
    - il GameServer comunica fine della partita -> viene gestita
    - l'utente vuole rigiocare -> verrà ricontattato GreetingServer
  */
  "Client"  should "be able to handle game end, player wants to play again" in {
    val testId: String = "5"
    //attore a cui il Controller invierà messaggi
    val controllerListener = TestProbe()
    //il mio client
    val client = system.actorOf(Props(new ClientToTest()), CLIENTACTOR_NAME+testId)
    //greetingServer
    val greetingServer = TestProbe()
    //avvia attore che ascolta il topic del greetingServer (GreetingServerTopicListener)
    system.actorOf(Props(new GreetingServerTopicListener(greetingServer.ref)), GREETING_TOPIC_LISTENER_NAME+testId )
    //GameServer
    val gameServer = TestProbe()
    //topic del GameServer
    val gsTopic :String= GAME_SERVER_TOPIC+testId
    //attore che invia i messaggi sul topic del gameserver
    val gameServerTopicSender = system.actorOf(Props(new OnGameTopicSenderActor(gsTopic, gameServer.ref)), GAME_TOPIC_SENDER_NAME+testId)

    //inizializzo il controller
    controllerInitCheck(controllerListener, client)

    //trick per skippare parti già testate ed andare dritto a WaitingGameServerTopic
    client ! JumpToWaitingGameServerTopic(greetingServer.ref, USERNAME)
    //controlla che sia avvenuto il setup
    expectMsgType[SetUpDoneWGST](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))

    //testo interazione di inizio partita -> ClientActor attende (anche) GameEnded
    testConnectionToGameServer(controllerListener, client, gameServer, gsTopic)

    //gameServer manda messaggio GameEnded e vengono controllati relativi ack
    testGameEndedInteraction(controllerListener, client, gameServer, gameServerTopicSender)

    //non dovrebbero ricevere ulteriori messaggi
    bothReceivesNoMessageCheck(controllerListener,gameServer)

    //comunico al client che utente vuole giocare di nuovo
    client ! PlayAgain(true)

    //dovrebbe essere possibile contattare il GreetingServer
    testNewGameRequest(client, greetingServer)
  }


  /*
    test della fase di gioco, nel caso in cui non sia il turno del player;
    dopodichè fingo la partita finisce (in pratica è l'ultimo turno)
   */
  "Client"  should "be able to handle other player's turn, the game ends" in {
    //id del test
    val testId: String = "7"
    //attore a cui il Controller invierà messaggi
    val controllerListener = TestProbe()
    //il mio client
    val client = system.actorOf(Props(new ClientToTest()), CLIENTACTOR_NAME + testId)
    //greetingServer
    val greetingServer = TestProbe()
    //avvia attore che ascolta il topic del greetingServer (GreetingServerTopicListener)
    system.actorOf(Props(new GreetingServerTopicListener(greetingServer.ref)), GREETING_TOPIC_LISTENER_NAME + testId)
    //GameServer
    val gameServer = TestProbe()
    //topic del GameServer
    val gsTopic: String = GAME_SERVER_TOPIC + testId
    //attore che invia i messaggi sul topic del gameserver
    val gameServerTopicSender = system.actorOf(Props(new OnGameTopicSenderActor(gsTopic, gameServer.ref)), GAME_TOPIC_SENDER_NAME + testId)

    //inizializzo il controller
    controllerInitCheck(controllerListener, client)

    //trick per skippare parti già testate ed andare dritto a WaitingInTurnPlayerNomination
    client ! JumpToWaitingInTurnPlayerNomination(greetingServer.ref, USERNAME, gameServer.ref, gsTopic)
    //controlla che sia avvenuto il setup
    expectMsgType[SetUpDoneWITPN](new FiniteDuration(WAIT_TIME_MESSAGES, TimeUnit.SECONDS))

    //testprobe che genera ActorRef del giocatore in turno
    val playerInTurn = TestProbe()

    //gameServer dichiara inizio del turno di un altro giocatore
    gameServer.send(gameServerTopicSender, PlayerTurnBegins(playerInTurn.ref))

    //ClientActor dovrebbe inviare un Ack al gameServer
    val state = gameServer.expectMsgType[PlayerTurnBeginAck](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))
    state must equal(PlayerTurnBeginAck())

    //in questo stato controller non dovrebbe ricevere messaggi
    actorReceivesNoMessageCheck(controllerListener)

    //testa il fineturno
    testTurnEnd(controllerListener, gameServer, gameServerTopicSender)

    //ClientActor in waitingInTurnPlayerNomination => terminiamo la partita

    //in questo stato gameServer e controllerListener non dovrebbero ricevere messaggi
    bothReceivesNoMessageCheck(gameServer, controllerListener)

    //gameServer manda messaggio GameEnded e vengono controllati relativi ack
    testGameEndedInteraction(controllerListener, client, gameServer, gameServerTopicSender)
  }



  /*
    test della fase di gioco, nel caso in cui sia il turno del player [caso 1 AcceptedWord];
    dopodichè termina la partita
   */
  "Client" should "be able to play his turn [AcceptedWord], poi faccio terminare la partita" in {
    //id del test
    val testId: String = "8"
    //attore a cui il Controller invierà messaggi
    val controllerListener = TestProbe()
    //il mio client
    val client = system.actorOf(Props(new ClientToTest()), CLIENTACTOR_NAME + testId)
    //greetingServer
    val greetingServer = TestProbe()
    //avvia attore che ascolta il topic del greetingServer (GreetingServerTopicListener)
    system.actorOf(Props(new GreetingServerTopicListener(greetingServer.ref)), GREETING_TOPIC_LISTENER_NAME + testId)
    //GameServer
    val gameServer = TestProbe()
    //topic del GameServer
    val gsTopic: String = GAME_SERVER_TOPIC + testId
    //attore che invia i messaggi sul topic del gameserver
    val gameServerTopicSender = system.actorOf(Props(new OnGameTopicSenderActor(gsTopic, gameServer.ref)), GAME_TOPIC_SENDER_NAME + testId)

    //inizializzo il controller
    controllerInitCheck(controllerListener, client)

    //trick per skippare parti già testate ed andare dritto a WaitingInTurnPlayerNomination
    client ! JumpToWaitingInTurnPlayerNomination(greetingServer.ref, USERNAME, gameServer.ref, gsTopic)
    //controlla che sia avvenuto il setup
    expectMsgType[SetUpDoneWITPN](new FiniteDuration(WAIT_TIME_MESSAGES, TimeUnit.SECONDS))

    //gameServer dichiara inizio del turno dell'utente
    gameServer.send(gameServerTopicSender, PlayerTurnBegins(client))

    //ClientActor dovrebbe rispondere con Ack al GameServer
    val state3 = gameServer.expectMsgType[PlayerTurnBeginAck](new FiniteDuration(WAIT_TIME_MESSAGES, TimeUnit.SECONDS))
    state3 must equal(PlayerTurnBeginAck())

    //ClientActor dovrebbe comunicare a utente che è il suo turno
    checkReceivedStringMessage(controllerListener, USER_TURN_BEGINS)

    //ClientActor in WaitingUserMakingMove

    //parola composta sul tabellone
    val boardTilePlayed: List[BoardTile] =  getBoardTileList
    //l'utente indica la propria mossa
    client ! UserMadeHisMove(WordMove(boardTilePlayed))

    //ClientActor dovrebbe inoltrare la mossa dell'utente al gameServer
    val state1 = gameServer.expectMsgType[ClientMadeMove](new FiniteDuration(WAIT_TIME_MESSAGES, TimeUnit.SECONDS))
    state1 must equal(ClientMadeMove(WordMove(boardTilePlayed)))

    //ClientActor in WaitingMoveAckFromGameServer

    //contenuto del messaggio di risposta del client
    val gsAnswer = WordAccepted(Vector(CardImpl("A"), CardImpl("A"), CardImpl("A")))

    gameServer.send(client, ClientMoveAck(gsAnswer))

    val state2 = controllerListener.expectMsgType[AcceptedWord](new FiniteDuration(WAIT_TIME_MESSAGES, TimeUnit.SECONDS))
    state2 must equal(AcceptedWord(Vector(CardImpl("A"), CardImpl("A"), CardImpl("A"))))

    //in questo stato gameServer non dovrebbe ricevere messaggi
    actorReceivesNoMessageCheck(gameServer)

    //testa il fineturno
    testTurnEnd(controllerListener, gameServer, gameServerTopicSender)

    //entrambi non dovrebbero ricevere messaggi
    bothReceivesNoMessageCheck(controllerListener, gameServer)

    //gameServer manda messaggio GameEnded e vengono controllati relativi ack
    testGameEndedInteraction(controllerListener, client, gameServer, gameServerTopicSender)
  }







  /*
     il test6 va per ultimo: termina l'ActorSystem quindi poi non puoi piu creare attori,
     deve essere l'ultimo test, valutare se eliminarlo
   */
  /*
    testo la capacità del Client gestire chiusura del gioco dopo la fine di una partita
    parto dall'attesa della risposta (negativa) dell'utente riguardo a fare nuova partita
   */
  "Client"  should "be able to handle game end, player doesn't want to play again" in {
    val testId: String = "6"
    //attore a cui il Controller invierà messaggi
    val controllerListener = TestProbe()
    //il mio client
    val client = system.actorOf(Props(new ClientToTest()), CLIENTACTOR_NAME+testId)
    //greetingServer
    val greetingServer = TestProbe()
    //avvia attore che ascolta il topic del greetingServer (GreetingServerTopicListener)
    system.actorOf(Props(new GreetingServerTopicListener(greetingServer.ref)), GREETING_TOPIC_LISTENER_NAME+testId )
    //GameServer
    val gameServer = TestProbe()
    //topic del GameServer
    val gsTopic :String= GAME_SERVER_TOPIC+testId
    //attore che invia i messaggi sul topic del gameserver
    val gameServerTopicSender = system.actorOf(Props(new OnGameTopicSenderActor(gsTopic, gameServer.ref)), GAME_TOPIC_SENDER_NAME+testId)

    //inizializzo il controller
    controllerInitCheck(controllerListener, client)

    //trick per skippare parti già testate ed andare dritto a WaitingUserChoosingWheterPlayAgainOrClosing
    client ! JumpToWaitingUserChoosingWheterPlayAgainOrClosing(greetingServer.ref, USERNAME, gameServer.ref, gsTopic)
    //controlla che sia avvenuto il setup
    expectMsgType[SetUpDoneWUCWPAOC](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))

    //comunico al client che utente vuole giocare di nuovo
    client ! PlayAgain(false)

    //client dovrebbe inviare Ack di ricezione messaggio GameEnded a GameServer
    val state = greetingServer.expectMsgType[DisconnectionToGreetingNotification](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))
    state must equal(DisconnectionToGreetingNotification())

    //greetingServer dovrebbe rispondere con un Ack al messaggio di disconnessione
    greetingServer.send(client, DisconnectionAck())

    //greetingServer non dovrebbe ricerver ulteriori messaggi in quanto il client dovrebbe essersi stoppato
    actorReceivesNoMessageCheck(greetingServer)
  }


  //METODI TEST CHIAMATI PIU VOLTE

  //testa interazione di fineturno: GameServer dichiara fineturno e Client invia ack a gameserver e controller
  private def testTurnEnd(controllerListener: TestProbe,
                          gameServer: TestProbe,
                          gameServerTopicSender:ActorRef):Unit = {
    //ClientActor in waitingTurnEndUpdates
    val playerRanking:List[(String,Int)] = getRanking
    val board: List[BoardTile] =  getBoardTileList

    //gameServer dichiara fine del turno di un altro giocatore
    gameServer.send(gameServerTopicSender, EndTurnUpdate(playerRanking, board))

    //controller dovrebbe ricevere un avviso di fineturno
    val state1 = controllerListener.expectMsgType[TurnEndUpdates](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))
    state1 must equal(TurnEndUpdates(playerRanking,board))
    //gameserver dovrebbe ricevere ack
    val state2 = gameServer.expectMsgType[EndTurnUpdateAck](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))
    state2 must equal(EndTurnUpdateAck())
  }

  //restituisce una lista con un ranking di player fittizi da usare come segnaposto in alcuni messaggi
  private def getRanking:List[(String,Int)] = List(("player1",1),("player2",2),("player3",3))

  //restituisce una List[BoardTile] da usare come segnaposto in alcuni messaggi
  private def getBoardTileList: List[BoardTile] = (for (x <- 1 to 17; y <- 1 to 17) yield BoardTileImpl(PositionImpl(x, y), DEFAULT_CARD)).toList


  //GameServer invia al client messaggio di terminazione della partita, poi vengono controllati i conseguenti ack
  private def testGameEndedInteraction(controllerListener: TestProbe,
                                       client: ActorRef,
                                       gameServer: TestProbe,
                                       gameServerTopicSender:ActorRef): Unit = {
    //nome del vincitore
    val winnerName: String= "vincitore"

    //GameServer comunica fine partita
    gameServer.send(gameServerTopicSender, GameEnded(winnerName,client))

    //client dovrebbe inviare Ack di ricezione messaggio GameEnded a GameServer
    val state = gameServer.expectMsgType[GameEndedAck](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))
    state must equal(GameEndedAck())

    //UI dovrebbe ricevere notifica di fine partita
    val state1 = controllerListener.expectMsgType[MatchEnded](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))
    val playerWon: Boolean = true
    state1 must equal(MatchEnded(winnerName, playerWon))
  }

  //Simula interazione UI-Client-GreetingServer dopo la chiamata OnLoginResponse
  private def testNewGameRequest(client: ActorRef,
                                 greetingServer: TestProbe):Unit = {
    //testo che utente possa effettuare richiesta per nuova partita
    client ! JoinQueue()
    greetingServer.expectMsgType[ConnectionToGreetingQuery](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))
    //questo evita che il client continui a inviare richieste, dando problemi ai test successivi
    greetingServer.send(client, ConnectionAnswer(true))
  }

  /*
   Simula prima interazione UI-Client-GameServer
    - Client deve essere in waitingGameServerTopic, attente primo messaggio da GameServer
    - Client risponde sia a Controller che a GameServer
  */
  private def testConnectionToGameServer(controllerListener: TestProbe,
                                         client: ActorRef,
                                         gameServer: TestProbe,
                                         gsTopic:String):Unit = {
    //questa sarà la mano del giocatore
    val hand: Vector[Card] = Vector(CardImpl("B"), CardImpl("C"), CardImpl("D"))
    val players: List[String] = List("player1","player2","player3")
    //il gameServer contatta il giocatore
    gameServer.send(client, MatchTopicListenQuery(gsTopic, "chatTopic", hand, players))

    //il controller dovrebbe ricevere un messaggio con la mano di carte e la lista dei giocatori
    val state2 = controllerListener.expectMsgType[OnMatchStart](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))
    state2 must equal(OnMatchStart(hand, players))

    //gameserver dovrebbe ricevere messaggio di ack in risposta
    val state3 = gameServer.expectMsgType[MatchTopicListenAck](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))
    state3 must equal(MatchTopicListenAck())
  }

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
    client ! UsernameChosen(USERNAME)
    checkReceivedStringMessage(controllerListener, ON_LOGIN_RESPONSE)

    //UI richiede di essere messo in coda per nuova partita
    client ! JoinQueue()
    greetingServer.expectMsgType[ConnectionToGreetingQuery](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))
  }

  //METODI DI UTILITY


  //spesso mi troverò a controllare che nessun altro attore oltre a Client riceva messaggi
  private def bothReceivesNoMessageCheck(actor1: TestProbe, actor2: TestProbe): Unit = {
    actorReceivesNoMessageCheck(actor1)
    actorReceivesNoMessageCheck(actor2)
  }

  //si assicura che l'attore indicato non riceva messaggi (per un certo dt)
  private def actorReceivesNoMessageCheck(actor: TestProbe):Unit = actor.expectNoMessage(new FiniteDuration(TIME_WITHOUT_MESSAGES,TimeUnit.SECONDS))

  //inizializza il controller ed effettua check su di esso
  private def controllerInitCheck(controllerListener: TestProbe,
                                  client: ActorRef): Unit = {
    val verbous: Boolean = false //non voglio vengano eseguite print in ControllerLogic
    Controller.init(client, TestMind(verbous, controllerListener.ref))
    checkReceivedStringMessage(controllerListener, START_GUI)
  }

  //controlla che receiver riceva il expectedMessage entro limiti di tempo prestabiliti
  private def checkReceivedStringMessage(receiver: TestProbe, expectedMessage:String):Unit = {
    var state = receiver.expectMsgType[String](new FiniteDuration(WAIT_TIME_MESSAGES,TimeUnit.SECONDS))
    state must equal(expectedMessage)
    //println("checkReceivedStrinMessage: " + state  + " == " + expectedMessage)
  }

}
