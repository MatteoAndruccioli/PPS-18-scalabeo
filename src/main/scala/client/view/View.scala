package client.view

import client.controller.Controller
import client.controller.Messages.ViewToClientMessages
import scalafx.application.{JFXApp, Platform}

object View extends JFXApp {

  val mainMenu: MainMenu = new MainMenu
  var gameBoard: GameView = _
  stage = mainMenu


  //da chiamare quando il greeting ha confermato il login
  def onLoginResponse(): Unit = {
    mainMenu.onLoginResponse()
  }

  def sendToClient(message: ViewToClientMessages): Unit = {
    Controller.sendToClient(message)
  }

  //da chiamare quando si può iniziare la partita
  def onMatchStart(): Unit = {
    Platform.runLater(() => {
      stage.close()
      gameBoard = new GameView()
      //updateHand(cards)
    })
  }

  //chiede all'utente di joinare la partita
  def askUserToJoinGame(): Unit = {
    mainMenu.askUserToJoinGame()
  }

  //Chiamato quando dalla gameboard si vuole tornare nel mainmenu
  def backToMainMenu(): Unit = {
    gameBoard.close()
    stage.show()
  }

  //chiamato quando inizia il turno dell'utente
  def userTurnBegins(): Unit = {
    //TODO: Fare in modo che l'utente possa interagire con la gui
  }

  //chiamato a fine turno quando il Gameserver broadcasta gli aggiornamenti, è la fine del turno del player per il GameServer
  def turnEndUpdates(word: List[(LetterTile, Int, Int)]): Unit = {
    //TODO: Qui va updatata la board facendo vedere la parola giocata
  }

  def endMyTurn(): Unit = {
    //TODO: Qui va chiamata la logica di fine turno
  }

}

class View {}

