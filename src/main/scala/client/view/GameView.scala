package client.view

import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.layout.GridPane
import scalafx.scene.{Group, Scene}

class GameView extends PrimaryStage {

  private val utilityPanel: UtilityPanel = new UtilityPanel
  private val boardAndPlayerPanel: BoardAndPlayerPanel = new BoardAndPlayerPanel
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

