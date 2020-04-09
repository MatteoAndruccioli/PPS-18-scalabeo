package client.controller

import akka.actor.ActorRef
import client.controller.Messages.ViewToClientMessages
import client.controller.Messages.ViewToClientMessages.UserMadeHisMove
import client.controller.MoveOutcome.ServerDown.{GameServerDown, GreetingServerDown}
import client.controller.MoveOutcome.{AcceptedWord, HandSwitchAccepted, HandSwitchRefused, PassReceived, RefusedWord, ServerDown, TimeoutReceived}
import client.view.{BoardInteraction, LetterStatus, LetterTile, View}
import model.{BoardTile, Card}
import shared.Move.WordMove

import scala.collection.mutable.ArrayBuffer

object Controller {

  private var _myTurn: Boolean = false
  private var clientRef: ActorRef = _
  private var _username: String = _

  def username_= (username: String): Unit = _username = username
  def username: String = _username

  def init(clientRef: ActorRef): Unit  = {
    this.clientRef = clientRef
    startGui()
  }

  private def startGui(): Unit = {
    new Thread(() => {
      View.main(Array[String]())
    }).start()
  }

  def sendToClient(message: ViewToClientMessages): Unit ={
    clientRef ! message
  }

  def onLoginResponse(): Unit = {
    View.onLoginResponse()
  }

  def askUserToJoinGame(): Unit = {
    View.askUserToJoinGame()
  }

  def onMatchStart(hand:ArrayBuffer[Card], players: List[String]): Unit = {
    View.onMatchStart(hand.map(c => (c.letter, c.score)), players)
    GameManager.newGame(hand)
  }

  def isMyTurn: Boolean = {
    this._myTurn
  }

  def userTurnBegins(): Unit = {
    this._myTurn = true
    View.userTurnBegins()
  }

  def endMyTurn(): Unit = {
    this._myTurn = false
  }

  def turnEndUpdates(ranking: List[(String,Int)], board:List[BoardTile]): Unit = {
    View.updateLeaderboard(ranking)
    GameManager.addPlayedWordAndConfirm(board)
    View.turnEndUpdates(board.map(b => (LetterTile(60, b.card.letter, b.card.score.toString, 0, LetterStatus.insertedConfirmed), b.position.row, b.position.col)))
  }

  def addCardToTile(position: Int, x: Int, y: Int): Unit = {
    GameManager.addCardToTile(position, x, y)
  }

  def collectLetters(): Unit = {
    GameManager.collectLetters()
  }

  def playWord(): Unit = {
    val playedWord = GameManager.getPlayedWord
    if(!playedWord.isEmpty)
      {
        playedWord.foreach(b => {
          print(b.card.letter)
        })
        sendToClient(UserMadeHisMove(WordMove(playedWord)))
      }
  }

  //metodo attraverso cui il Client comunica al controller l'esito della mossa inviata al GameServer
  def moveOutcome[A >: MoveOutcome](outcome: A):Unit = outcome match {
    case _: RefusedWord => {takeLettersBackInHand(); userTurnContinues()}
    case _: HandSwitchRefused => {userTurnContinues()}
    case _: AcceptedWord => {updateHand(outcome.asInstanceOf[AcceptedWord].hand); View.confirmPlay(); GameManager.confirmPlay()}
    case _: HandSwitchAccepted => {updateHand(outcome.asInstanceOf[HandSwitchAccepted].hand); endMyTurn()}
    case _: PassReceived => {endMyTurn()}
    case _: TimeoutReceived => {endMyTurn()}
  }

  private def updateHand(hand:ArrayBuffer[Card]): Unit = {
    View.updateHand(hand.map(c => (c.letter, c.score)))
    GameManager.changeHand(hand)
  }

  private def takeLettersBackInHand(): Unit = {
    View.getLettersBackFromBoard();
    GameManager.collectLetters()
  }

  def userTurnContinues(): Unit = {
    _myTurn = true
    View.userTurnContinues()
  }

  def isMulliganAvailable: Boolean = {
    GameManager.isMulliganAvailable()
  }

  def onConnectionFailed(): Unit = {
    View.terminate()
  }

  def serversDown(server: ServerDown):Unit = {
    server match {
      case _: GreetingServerDown => //TODO far visualizzare terminazione della partita a causa di un errore, chiudere partita
      case _: GameServerDown => //TODO far visualizzare terminazione della partita a causa di un errore, tornare a schermata scelta modalità
    }
    println("**** Errore: " + server + " crollato")
  }

  def matchEnded(player: String, playerWon:Boolean): Unit =  {
    endMyTurn()
    BoardInteraction.reset()
    View.matchEnded(player, playerWon)
  }

  def terminate(): Unit = {
    View.terminate()
  }
}

// tipo dell'esito di una mossa, contiene informazioni che indicano la risposta del server alla mossa compiuta dall'utente
sealed trait MoveOutcome
object MoveOutcome{
  //casi in cui la mossa effettuata non viene accettata dal'utente
  case class RefusedWord() extends MoveOutcome //utente aveva indicato la composizione di una parola che viene rifìutata
  case class HandSwitchRefused() extends MoveOutcome //utente aveva richiesto un cambio delle tessere nella mano, rifiutato dal GameServer

  //casi in cui la mossa viene accettata dall'utente
  //utente aveva indicato la composizione di una parola che viene accettata, GameServer passa inoltre la nuova mano di tessere disponibili all'utente
  case class AcceptedWord(hand:ArrayBuffer[Card]) extends MoveOutcome
  //utente aveva richiesto un cambio delle tessere nella mano, GameServer passa inoltre la nuova mano di tessere disponibili all'utente
  case class HandSwitchAccepted(hand:ArrayBuffer[Card]) extends MoveOutcome
  //utente aveva espresso intenzione di passare il turno, GameServer ne ha preso atto
  case class PassReceived() extends MoveOutcome
  //timer è scaduto, GameServer ne ha preso atto
  case class TimeoutReceived() extends MoveOutcome

  sealed trait ServerDown
  object ServerDown{
    case class GreetingServerDown() extends ServerDown
    case class GameServerDown() extends ServerDown
  }

}


