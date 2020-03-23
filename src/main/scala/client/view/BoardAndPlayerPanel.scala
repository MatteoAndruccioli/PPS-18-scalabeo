package client.view

import scalafx.scene.layout.{BorderPane, HBox, VBox}

class BoardAndPlayerPanel extends BorderPane {

  private val board = new BoardPanel
  stylesheets = List("/style/BPStyle.css")
  styleClass += "body"

  val myHand: HBox = new HBox(8) {
    styleClass += "my-hand"
  }

  val opponentHandTop: HBox = new HBox(8) {
    styleClass += "top-opponent"
  }

  val opponentHandLeft: VBox = new VBox(8) {
    styleClass += "left-opponent"
  }

  val opponentHandRight: VBox = new VBox(8) {
    styleClass += "right-opponent"
  }
  center = board
  left = opponentHandLeft
  right = opponentHandRight
  top = opponentHandTop
  bottom = myHand
}
