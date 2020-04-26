package client

import akka.actor.ActorRef
import client.controller.{ControllerLogic, MoveOutcome}
import client.controller.MoveOutcome.ServerDown
import model.{BoardTile, Card}

/** Stringhe utilizzate in TestLogic come messaggi per il Test del ClientActor
 *  - ogni stringa indica l'avvenuta invocazione del metodo corrispondente
 */
object ClientTestConstants {
  val START_GUI:String = "startGui"
  val ON_LOGIN_RESPONSE:String = "onLoginResponse"
  val ASK_USER_TO_JOIN_GAME:String = "askUserToJoinGame"
  val USER_TURN_BEGINS:String = "userTurnBegins"
  val COLLECT_LETTERS:String = "collectLetters"
  val PLAY_WORD:String = "playWord"
  val TAKE_LETTERS_BACK_IN_HAND:String = "takeLettersBackInHand"
  val USER_TURN_CONTINUES:String = "userTurnContinues"
  val ON_CONNECTION_FAILED:String = "onConnectionFailed"
  val PLAYER_LEFT:String = "playerLeft"
  val TERMINATE:String = "terminate"
  val IS_MULLIGAN_AVAILABLE:String = "isMulliganAvailable"
}

/** Tipo dei messaggi con parametro usati per il test di ClientActor */
sealed trait ClientTestMessage
/** Implementazione di messaggi usati per il test di ClientActor*/
object ClientTestMessage {

  /** inviato dopo l'invocaione del metodo onMatchStart
   *  @param hand Carte che compongono la mano del player
   *  @param players elenco dei giocatori
   */
  case class OnMatchStart(hand:Vector[Card], players: List[String]) extends ClientTestMessage

  /** Messaggio contenente aggiornamenti di fineturno
   *  @param ranking classifica aggiornata dei giocatori
   *  @param board tabellone aggiornato
   */
  case class TurnEndUpdates(ranking: List[(String,Int)], board:List[BoardTile]) extends ClientTestMessage

  /** è stato richiesto posizionamento di una tessera sul tabellone
   *  @param position posizione della carta in mano
   *  @param x x della tessera sul tabellone
   *  @param y y della tessera sul tabellone
   */
  case class AddCardToTile(position: Int, x: Int, y: Int) extends ClientTestMessage

  /** Indica il risultato della giocata restituito dal GameServer
   *  @param outcome risultato della giocata
   *  @tparam A tipo di risultato della giocata
   */
  case class MoveOutcomeMessage[A >: MoveOutcome](outcome: A) extends ClientTestMessage

  /** Aggiornamento della mano del giocatore
   *  @param hand mano del giocatore
   */
  case class UpdateHand(hand:Vector[Card]) extends ClientTestMessage

  /** notifica del crollo di un Server
   *  @param server indica il server crollato
   */
  case class ServersDownMessage(server: ServerDown) extends ClientTestMessage

  /** Notifica di terminazione partita
   *  @param player nome del giocatore vincitore
   *  @param playerWon indica se è il player corrente ad aver vinto
   */
  case class MatchEnded(player: String, playerWon:Boolean) extends ClientTestMessage

  /** Notifica nuovo messaggio chat
   * @param sender username del mittente
   * @param message testo del messaggio
   */
  case class ShowInChat(sender: String, message: String) extends ClientTestMessage

}

import ClientTestMessage._
import ClientTestConstants._


/** implementazione di ControllerLogic server a scopo di test
 * - ogni metodo non effettua elaborazione, ma quando viene invocato invia un messaggio all'attore receiver
 *    indicato nel costruttore, in questo modo è possibile verificare il momento in cui ogni metodo viene invocato
 * @param receiver attore a cui invio messaggi
 */
case class TestLogic(receiver: ActorRef) extends ControllerLogic{

  override def startGui(): Unit = {
    receiver ! START_GUI
  }

  override def onLoginResponse(): Unit = {
    receiver ! ON_LOGIN_RESPONSE
  }

  override def askUserToJoinGame(): Unit = {
    receiver ! ASK_USER_TO_JOIN_GAME
  }

  override def onMatchStart(hand: Vector[Card], players: List[String]): Unit = {
    receiver ! OnMatchStart(hand, players)
  }

  override def userTurnBegins(): Unit = {
    receiver ! USER_TURN_BEGINS
  }

  override def turnEndUpdates(ranking: List[(String, Int)], board: List[BoardTile]): Unit = {
    receiver ! TurnEndUpdates(ranking, board)
  }

  override def addCardToTile(position: Int, x: Int, y: Int): Unit = {
    receiver ! AddCardToTile(position, x, y)
  }

  override def collectLetters(): Unit = {
    receiver ! COLLECT_LETTERS
  }

  override def playWord(): Unit = {
    receiver ! PLAY_WORD
  }

  override def moveOutcome[A >: MoveOutcome](outcome: A): Unit = {
    receiver ! outcome
  }

  override def updateHand(hand: Vector[Card]): Unit = {
    receiver ! UpdateHand(hand)
  }

  override def takeLettersBackInHand(): Unit = {
    receiver ! TAKE_LETTERS_BACK_IN_HAND
  }

  override def userTurnContinues(): Unit = {
    receiver ! USER_TURN_CONTINUES
  }

  override def isMulliganAvailable: Boolean = {
    receiver ! IS_MULLIGAN_AVAILABLE
    true
  }

  override def onConnectionFailed(): Unit = {
    receiver ! ON_CONNECTION_FAILED
  }

  override def serversDown(server: ServerDown): Unit = {
    receiver ! ServersDownMessage(server)
  }

  override def matchEnded(player: String, playerWon: Boolean): Unit = {
    receiver ! MatchEnded(player,playerWon)
  }

  override def playerLeft(): Unit = {
    receiver ! PLAYER_LEFT
  }

  override def terminate(): Unit = {
    receiver ! TERMINATE
  }

  override def showInChat(sender: String, message: String): Unit = {
    receiver ! ShowInChat(sender, message)
  }

}

