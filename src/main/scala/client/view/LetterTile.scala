package client.view

import client.controller.Controller
import client.view.LetterStatus.LetterStatus
import scalafx.application.Platform
import scalafx.scene.Node
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout.{HBox, StackPane}
import scalafx.scene.text.Text
import scalafx.Includes._

case class LetterTile(size: Double, letter: String, letterValue: String, var position: Int = 0, var letterStatus: LetterStatus = LetterStatus.inHand) extends StackPane {
  private val TILE_SIZE = 30
  private val START_IMAGE_PATH = "/assets/start.png"
  private val LETTER_TRANSLATION_VALUE = -10
  maxWidth = size
  maxHeight = size
  prefWidth = size
  prefHeight = size

  stylesheets = List("/style/TileStyle.css")
  styleClass += "letter-tile"

  val letterText: Node = letter match {
    case "[a-zA-Z]" => new ImageView(new Image(START_IMAGE_PATH)) {
      fitHeight = TILE_SIZE
      fitWidth = TILE_SIZE
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

  onMousePressed = (e: MouseEvent)  => {
    BoardInteraction.select(this)
    if (Controller.isMyTurn && (letterStatus == LetterStatus.inHand)) {
      this.translateY = LETTER_TRANSLATION_VALUE
    }
    e.consume()
  }

  def unselect(): Unit = {
    Platform.runLater(() => {
      this.translateY = 0
    })
  }

}

object LetterStatus extends Enumeration {
  type LetterStatus = Value
  val inHand, inserted, insertedConfirmed, placeHolder = Value
}


