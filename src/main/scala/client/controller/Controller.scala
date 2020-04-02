package client.controller

import akka.actor.ActorRef
import client.controller.Messages.ViewToClientMessages
import client.controller.MoveOutcome.{AcceptedWord, HandSwitchAccepted, HandSwitchRefused, PassReceived, RefusedWord, TimeoutReceived}
import client.view.View
import model.Card

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

  def onMatchStart(): Unit = {
    View.onMatchStart()
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

  def turnEndUpdates(): Unit = {
  }

  def addCardToTile(position: Int, x: Int, y: Int): Unit = {
    GameManager.addCardToTile(position, x, y)
  }

  def collectLetters(): Unit = {
    GameManager.collectLetters()
  }

  //metodo attraverso cui il Client comunica al controller l'esito della mossa inviata al GameServer
  def moveOutcome[A >: MoveOutcome](outcome: A):Unit = outcome match {
    case _: RefusedWord => _
    case _: HandSwitchRefused => _
    case _: AcceptedWord => _
    case _: HandSwitchAccepted => _
    case _: PassReceived => _
    case _: TimeoutReceived => _
  }

  def onConnectionFailed : Unit = ???

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


