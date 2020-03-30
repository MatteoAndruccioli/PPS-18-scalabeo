package client.view

import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.layout.GridPane
import scalafx.scene.{Group, Scene}

import scala.collection.mutable.ArrayBuffer

class GameView extends PrimaryStage {

  private val utilityPanel: UtilityPanel = new UtilityPanel
  private val boardAndPlayerPanel: BoardAndPlayerPanel = new BoardAndPlayerPanel(ArrayBuffer(("s", 1), ("c", 1), ("a", 1), ("l", 1), ("a", 1), ("b", 1), ("e", 1), ("o", 1)))
  title = "Scalabeo"
  scene = new Scene(1280, 720) {
    root = new Group() {
      children = new GridPane() {
        add(utilityPanel, 1, 0)
        add(boardAndPlayerPanel, 0, 0)
      }
    }
  }
  resizable = false
  sizeToScene()
  show()
}

