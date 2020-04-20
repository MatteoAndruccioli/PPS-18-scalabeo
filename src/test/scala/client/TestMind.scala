package client

import akka.actor.ActorRef
import client.controller.ControllerLogic.StupidMind
import client.controller.MoveOutcome
import client.controller.MoveOutcome.ServerDown
import model.{BoardTile, Card}
import scala.collection.mutable.ArrayBuffer

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

sealed trait ClientTestMessage
object ClientTestMessage {

  case class OnMatchStart(hand:ArrayBuffer[Card], players: List[String]) extends ClientTestMessage
  case class TurnEndUpdates(ranking: List[(String,Int)], board:List[BoardTile]) extends ClientTestMessage
  case class AddCardToTile(position: Int, x: Int, y: Int) extends ClientTestMessage
  case class MoveOutcomeMessage[A >: MoveOutcome](outcome: A) extends ClientTestMessage
  case class UpdateHand(hand:ArrayBuffer[Card]) extends ClientTestMessage
  case class ServersDownMessage(server: ServerDown) extends ClientTestMessage
  case class MatchEnded(player: String, playerWon:Boolean) extends ClientTestMessage
  case class ShowInChat(sender: String, message: String) extends ClientTestMessage

}

import ClientTestMessage._
import ClientTestConstants._
case class TestMind(verbose: Boolean = true, receiver: ActorRef) extends StupidMind(verbose){

  override def startGui(): Unit = {
    receiver ! START_GUI
    super.startGui()
  }

  override def onLoginResponse(): Unit = {
    receiver ! ON_LOGIN_RESPONSE
    super.onLoginResponse()
  }

  override def askUserToJoinGame(): Unit = {
    receiver ! ASK_USER_TO_JOIN_GAME
    super.askUserToJoinGame()
  }

  override def onMatchStart(hand: ArrayBuffer[Card], players: List[String]): Unit = {
    receiver ! OnMatchStart(hand, players)
    super.onMatchStart(hand,players)
  }

  override def userTurnBegins(): Unit = {
    receiver ! USER_TURN_BEGINS
    super.userTurnBegins()
  }

  override def turnEndUpdates(ranking: List[(String, Int)], board: List[BoardTile]): Unit = {
    receiver ! TurnEndUpdates(ranking, board)
    super.turnEndUpdates(ranking, board)
  }

  override def addCardToTile(position: Int, x: Int, y: Int): Unit = {
    receiver ! AddCardToTile(position, x, y)
    super.addCardToTile(position, x, y)
  }

  override def collectLetters(): Unit = {
    receiver ! COLLECT_LETTERS
    super.collectLetters()
  }

  override def playWord(): Unit = {
    receiver ! PLAY_WORD
    super.playWord()
  }

  override def moveOutcome[A >: MoveOutcome](outcome: A): Unit = {
    receiver ! outcome
    super.moveOutcome(outcome)
  }

  override def updateHand(hand: ArrayBuffer[Card]): Unit = {
    receiver ! UpdateHand(hand)
    super.updateHand(hand)
  }

  override def takeLettersBackInHand(): Unit = {
    receiver ! TAKE_LETTERS_BACK_IN_HAND
    super.takeLettersBackInHand()
  }

  override def userTurnContinues(): Unit = {
    receiver ! USER_TURN_CONTINUES
    super.userTurnContinues()
  }

  override def isMulliganAvailable: Boolean = {
    receiver ! IS_MULLIGAN_AVAILABLE
    super.isMulliganAvailable
  }

  override def onConnectionFailed(): Unit = {
    receiver ! ON_CONNECTION_FAILED
    super.onConnectionFailed()
  }

  override def serversDown(server: ServerDown): Unit = {
    receiver ! ServersDownMessage(server)
    super.serversDown(server)
  }

  override def matchEnded(player: String, playerWon: Boolean): Unit = {
    receiver ! MatchEnded(player,playerWon)
    super.matchEnded(player, playerWon)
  }

  override def playerLeft(): Unit = {
    receiver ! PLAYER_LEFT
    super.playerLeft()
  }

  override def terminate(): Unit = {
    receiver ! TERMINATE
    super.terminate()
  }

  override def showInChat(sender: String, message: String): Unit = {
    receiver ! ShowInChat(sender, message)
    super.showInChat(sender, message)
  }

}

