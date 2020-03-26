package client.controller

import akka.actor.ActorRef
import client.controller.Messages.ViewToClientMessages
import client.view.View

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

}


