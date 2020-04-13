package client.view

import client.controller.Controller
import client.controller.Messages.ViewToClientMessages
import client.controller.Messages.ViewToClientMessages.{PlayAgain, UserExited}
import scalafx.application.{JFXApp, Platform}

import scala.collection.mutable.ArrayBuffer

object View extends JFXApp {

  var mainMenu: MainMenu = new MainMenu
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
  def onMatchStart(cards: ArrayBuffer[(String, Int)], players:List[String]): Unit = {
    Platform.runLater(() => {
      if(stage != null)
        stage.close()
      gameBoard = new GameView(cards, players)
      //updateHand(cards)
    })
  }

  //chiede all'utente di joinare la partita
  def askUserToJoinGame(): Unit = {
    mainMenu.askUserToJoinGame()
  }

  //Chiamato quando dalla gameboard si vuole tornare nel mainmenu
  def backToMainMenu(): Unit = {
    Platform.runLater(() =>{
      gameBoard.close()
      mainMenu = new MainMenu
      mainMenu.onLoginResponse()
      stage = mainMenu
      stage.show()
    })
  }

  def playerLeft(): Unit = {
    Platform.runLater(() =>{
      new Dialog("A player left the game, in 5 seconds you will be redirected to the main menu.")
        .autoClose(Option(gameBoard), () => {
          mainMenu = new MainMenu
          mainMenu.onLoginResponse()
          stage = mainMenu
          stage.show()
        })
        .show()
    })
  }

  def greetingDisconnected(): Unit = {
    Platform.runLater(() =>{
      new Dialog("The main server has crashed, the game will exit in 5 seconds.")
        .autoClose(Option(gameBoard), () => {
          Controller.exit()
        })
        .show()
    })
  }

  def gameServerDisconnected(): Unit = {
    Platform.runLater(() =>{
      new Dialog("The game server has crashed, in 5 seconds you will be redirected to the main menu.")
        .autoClose(Option(gameBoard), () => {
          mainMenu = new MainMenu
          mainMenu.onLoginResponse()
          stage = mainMenu
          stage.show()
        })
        .show()
    })
  }

  //chiamato quando inizia il turno dell'utente
  def userTurnBegins(): Unit = {
    gameBoard.disableMulliganButton(!Controller.isMulliganAvailable)
    gameBoard.startTurn()
    gameBoard.restartTimer()
  }

  //chiamato a fine turno quando il Gameserver broadcasta gli aggiornamenti, è la fine del turno del player per il GameServer
  def turnEndUpdates(word: List[(LetterTile, Int, Int)]): Unit = {
    gameBoard.updateBoard(word)
  }

  def endMyTurn(): Unit = {
    Controller.endMyTurn()
  }

  def updateHand(cards: ArrayBuffer[(String, Int)]): Unit = {
    BoardInteraction.updateHand(cards)
  }

  def getLettersBackFromBoard(): Unit = {
    BoardInteraction.collectLetters()
  }

  def userTurnContinues(): Unit = {
    gameBoard.resumeTimer()
  }

  def updateLeaderboard(ranking: List[(String, Int)]): Unit = {
    gameBoard.updateLeaderboard(ranking)
  }

  def confirmPlay(): Unit = {
    BoardInteraction.confirmPlay()
  }

  def matchEnded(player: String, playerWon: Boolean): Unit = {
    var winnerString = ""
    if(playerWon) {
      winnerString = "Hai vinto!!! Vuoi giocare di nuovo?"
    } else {
      winnerString = player + " ha vinto. Vuoi giocare di nuovo?"
    }
    Platform.runLater(() => {
        new Dialog(winnerString)
          .addYesNoButtons(
            () => {
              View.sendToClient(PlayAgain(true))
              View.backToMainMenu()
            },
            () => {
              View.sendToClient(PlayAgain(false))
              View.terminate()
            }).show()
      })
  }

  def terminate(): Unit  = {
    View.sendToClient(UserExited())
  }

}

class View {}

