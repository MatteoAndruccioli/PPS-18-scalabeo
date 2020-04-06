package client.view

import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.Platform
import scalafx.scene.layout.GridPane
import scalafx.scene.{Group, Scene}

import scala.collection.mutable.ArrayBuffer

class GameView(users: List[String]) extends PrimaryStage {

  private val utilityPanel: UtilityPanel = new UtilityPanel
  private val boardAndPlayerPanel: BoardAndPlayerPanel = new BoardAndPlayerPanel(ArrayBuffer(("s", 1), ("c", 1), ("a", 1), ("l", 1), ("a", 1), ("b", 1), ("e", 1), ("o", 1)))
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

  def matchEnded(player: String):Unit = {
    Platform.runLater(() => {
      new Dialog(player + " ha vinto, tra poco sarai riportato al menu principale").autoClose(Option(this)).show()
    })
  }


}

