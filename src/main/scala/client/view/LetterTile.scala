package client.view

import client.view.LetterStatus.LetterStatus
import scalafx.scene.Node
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{HBox, StackPane}
import scalafx.scene.text.Text

case class LetterTile(size: Double, letter: String, letterValue: String, var position: Int = 0, var letterStatus: LetterStatus = LetterStatus.inHand) extends StackPane {
  maxWidth = size
  maxHeight = size
  prefWidth = size
  prefHeight = size

  stylesheets = List("/style/TileStyle.css")
  styleClass += "letter-tile"

  val letterText: Node = letter match {
    case "[a-zA-Z]" => new ImageView(new Image("/assets/start.png")) {
      fitHeight = 30
      fitWidth = 30
    }
    case _ => new Text(letter.toUpperCase) {
      styleClass += "letter-tile-text-hand"
    }
  }

  val valueText: Text = new Text(letterValue) {
    styleClass += "letter-tile-value-text"
  }

  val valueContainer: HBox = new HBox(valueText) {
    styleClass += "value-container"
  }

  children = List(valueContainer, letterText)

}

object LetterStatus extends Enumeration {
  type LetterStatus = Value
  val inHand, inserted, insertedConfirmed, placeHolder = Value
}


