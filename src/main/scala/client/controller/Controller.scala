package client.controller

import akka.actor.ActorRef
import client.controller.Messages.ViewToClientMessages
import client.controller.MoveOutcome.ServerDown
import model.{BoardTile, Card}

object Controller {

  private var _myTurn: Boolean = false
  private var clientRef: ActorRef = _
  private var _username: String = _
  private var mind: ControllerLogic = _

  def username_= (username: String): Unit = _username = username
  def username: String = _username

  def init(clientRef: ActorRef, mind: ControllerLogic): Unit  = {
    this.clientRef = clientRef
    this.mind = mind
    mind.startGui()
  }

  def sendToClient(message: ViewToClientMessages): Unit ={
    clientRef ! message
  }

  def onLoginResponse(): Unit = mind.onLoginResponse()

  def askUserToJoinGame(): Unit = mind.askUserToJoinGame()

  def onMatchStart(hand:Vector[Card], players: List[String]): Unit = mind.onMatchStart(hand, players)


  def isMyTurn: Boolean = {
    this._myTurn
  }

  def userTurnBegins(): Unit = {
    this._myTurn = true
    mind.userTurnBegins()
  }

  def endMyTurn(): Unit = {
    this._myTurn = false
  }

  def turnEndUpdates(ranking: List[(String,Int)], board:List[BoardTile]): Unit = mind.turnEndUpdates(ranking,board)

  def addCardToTile(position: Int, x: Int, y: Int): Unit = mind.addCardToTile(position, x, y)

  def collectLetters(): Unit = mind.collectLetters()

  def playWord(): Unit = mind.playWord()


  //metodo attraverso cui il Client comunica al controller l'esito della mossa inviata al GameServer
  def moveOutcome[A >: MoveOutcome](outcome: A):Unit = mind.moveOutcome(outcome)

  def userTurnContinues(): Unit = {
    _myTurn = true
    mind.userTurnContinues()
  }

  //aggiunto per userTurnContinue in controllerLogic
  def setMyTurn():Unit = {
    _myTurn = true
  }

  def showInChat(sender: String, message: String): Unit = {
    mind.showInChat(sender, message)
  }

  def isMulliganAvailable: Boolean = mind.isMulliganAvailable

  def onConnectionFailed(): Unit = mind.onConnectionFailed()

  def serversDown(server: ServerDown):Unit = mind.serversDown(server)

  def matchEnded(player: String, playerWon:Boolean): Unit =  mind.matchEnded(player, playerWon)

  def playerLeft(): Unit = mind.playerLeft()

  def terminate(): Unit = mind.terminate()

  def exit(): Unit = {
    System.exit(0)
  }
}

// tipo dell'esito di una mossa, contiene informazioni che indicano la risposta del server alla mossa compiuta dall'utente
/** MoveOutcome contiene gli esiti che risultano da una mossa. Viene mandato dal server quando un giocatore effettua la
 * propria mossa.
 *
 */
sealed trait MoveOutcome
object MoveOutcome{

  /** La parola giocata dall'utente non è valida, quindi viene rifiutata.
   *
   */
  case class RefusedWord() extends MoveOutcome

  /** La mano dell'utente non viene cambiata perché non soddisfa i requisiti.
   *
   */
  case class HandSwitchRefused() extends MoveOutcome

  /** La parola dell'utente viene accettata e viene anche mandata la nuova mano dell'utente.
   *
   * @param hand la nuova mano dell'utente
   */
  case class AcceptedWord(hand:Vector[Card]) extends MoveOutcome

  /** Lo scambio mano dell'utente viene accettato e quindi gli viene passata la nuova mano.
   *
   * @param hand la nuova mano dell'utente
   */
  case class HandSwitchAccepted(hand:Vector[Card]) extends MoveOutcome

  /** Il passo dell'utente viene accettato.
   *
   */
  case class PassReceived() extends MoveOutcome

  /** Viene confermato il timeout del giocatore.
   *
   */
  case class TimeoutReceived() extends MoveOutcome

  /** ServerDown specifica i vari errori che si possono riscontrare lato server.
   *
   */
  sealed trait ServerDown
  object ServerDown{

    /** Il GreetingServer per un qualche motivo va down.
     *
     */
    case class GreetingServerDown() extends ServerDown

    /** Il GameServer per un qualche motivo va down.
     *
     */
    case class GameServerDown() extends ServerDown
  }

}


