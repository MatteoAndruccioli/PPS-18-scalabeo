package client.view

import client.controller.Controller
import client.controller.Messages.ViewToClientMessages
import client.controller.Messages.ViewToClientMessages.{PlayAgain, UserExited}
import scalafx.application.{JFXApp, Platform}

import scala.collection.mutable.ArrayBuffer

object View extends JFXApp {
  private val PLAYER_EXITED_DIALOG_TEXT = "A player left the game, in 5 seconds you will be redirected to the main menu."
  private val SERVER_CRASHED_DIALOG_TEXT = "The main server has crashed, the game will exit in 5 seconds."
  private val GAME_SERVER_CRASHED_TEXT = "The game server has crashed, in 5 seconds you will be redirected to the main menu."
  var mainMenu: MainMenu = new MainMenu
  var gameBoard: GameView = _
  stage = mainMenu

  def onLoginResponse(): Unit = {
    mainMenu.onLoginResponse()
  }

  def sendToClient(message: ViewToClientMessages): Unit = {
    Controller.sendToClient(message)
  }

  def onMatchStart(cards: ArrayBuffer[(String, Int)], players:List[String]): Unit = {
    Platform.runLater(() => {
      if(stage != null)
        stage.close()
      gameBoard = new GameView(cards, players)
    })
  }

  def askUserToJoinGame(): Unit = {
    mainMenu.askUserToJoinGame()
  }

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
      new Dialog(PLAYER_EXITED_DIALOG_TEXT)
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
      new Dialog(SERVER_CRASHED_DIALOG_TEXT)
        .autoClose(Option(gameBoard), () => {
          Controller.exit()
        })
        .show()
    })
  }

  def gameServerDisconnected(): Unit = {
    Platform.runLater(() =>{
      new Dialog(GAME_SERVER_CRASHED_TEXT)
        .autoClose(Option(gameBoard), () => {
          mainMenu = new MainMenu
          mainMenu.onLoginResponse()
          stage = mainMenu
          stage.show()
        })
        .show()
    })
  }

  def userTurnBegins(): Unit = {
    Platform.runLater(() => {
      gameBoard.disableMulliganButton(!Controller.isMulliganAvailable)
      gameBoard.startTurn()
      gameBoard.restartTimer()
      showEventMessage("E' il tuo turno!")
    })
  }

  def turnEndUpdates(word: List[(LetterTile, Int, Int)]): Unit = {
    gameBoard.updateBoard(word)
  }

  def showInChat(sender: String, message: String): Unit = {
    Platform.runLater(() => {
      gameBoard.showInChat(sender, message)
    })
  }

  def showEventMessage(message: String): Unit = {
    Platform.runLater(() => {
      gameBoard.showEventMessage(message)
    })
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
    Platform.exit()
  }

}

class View {}

