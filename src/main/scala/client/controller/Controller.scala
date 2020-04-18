package client.controller

import akka.actor.ActorRef
import client.controller.Messages.ViewToClientMessages
import client.controller.Messages.ViewToClientMessages.UserMadeHisMove
import client.controller.MoveOutcome.ServerDown.{GameServerDown, GreetingServerDown}
import client.controller.MoveOutcome.{AcceptedWord, HandSwitchAccepted, HandSwitchRefused, PassReceived, RefusedWord, ServerDown, TimeoutReceived}
import client.view.{BoardInteraction, LetterStatus, LetterTile, View}
import model.{BoardTile, Card}
import scalafx.application.Platform
import shared.Move.WordMove

import scala.collection.mutable.ArrayBuffer

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

  def onMatchStart(hand:ArrayBuffer[Card], players: List[String]): Unit = mind.onMatchStart(hand, players)


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


