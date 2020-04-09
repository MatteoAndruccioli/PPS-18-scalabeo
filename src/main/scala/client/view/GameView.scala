package client.view

import client.controller.Messages.ViewToClientMessages.PlayAgain
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.Platform
import scalafx.scene.layout.GridPane
import scalafx.scene.{Group, Scene}
import scalafx.Includes._

import scala.collection.mutable.ArrayBuffer

class GameView(cards: ArrayBuffer[(String, Int)], users: List[String]) extends PrimaryStage {

  private val utilityPanel: UtilityPanel = new UtilityPanel
  private val boardAndPlayerPanel: BoardAndPlayerPanel = new BoardAndPlayerPanel(cards)
  private val legendPanel = new LegendPanel(users)
  title = "Scalabeo"
  scene = new Scene(1280, 720) {
    root = new Group() {
      children = new GridPane() {
        add(legendPanel, 1, 0)
        add(utilityPanel, 2, 0)
        add(boardAndPlayerPanel, 0, 0)
      }
    }
  }
  resizable = false
  sizeToScene()
  show()

  def updateBoard(word: List[(LetterTile, Int, Int)]): Unit = {
    boardAndPlayerPanel.updateBoard(word)
  }

  def disableMulliganButton(condition: Boolean): Unit = {
    utilityPanel.disableMulliganButton(condition)
  }

  def startTurn(): Unit = {
    utilityPanel.startTurn()
  }

  def restartTimer(): Unit = {
    utilityPanel.restartTimer()
  }

  def pauseTimer(): Unit = {
    utilityPanel.pauseTimer()
  }

  def resumeTimer(): Unit = {
    utilityPanel.resumeTimer()
  }

  def updateLeaderboard(ranking: List[(String, Int)]): Unit = {
    legendPanel.updateLeaderboard(ranking)
  }

  //TODO: Capire dove far tornare il player dopo che ha premuto si.
  def matchEnded(player: String, playerWon: Boolean):Unit = {
    if(playerWon) {
      Platform.runLater(() => {
        new Dialog("Hai vinto!!! Vuoi giocare di nuovo?")
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
    } else {
      Platform.runLater(() => {
      new Dialog(player + " ha vinto. Vuoi giocare di nuovo?")
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
  }

  onCloseRequest = handle {
    View.terminate()
  }
}

