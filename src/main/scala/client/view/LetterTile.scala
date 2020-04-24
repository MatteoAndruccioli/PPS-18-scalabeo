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

/** Classe che modella le tessere di gioco.
 *
 * @param size la grandezza in pixel della tessera
 * @param letter il carattere della lettera
 * @param letterValue il valore della tessera
 * @param position la posizione della tessera se in mano al giocatore
 * @param letterStatus lo stato della tessera
 */
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

  /** Metodo per deselezionare una tessera selezionata in precedenza.
   *
   */
  def unselect(): Unit = {
    Platform.runLater(() => {
      this.translateY = 0
    })
  }

}

/** Enumerazione che contiene gli stati possibili di una tessera.
 *
 */
object LetterStatus extends Enumeration {
  type LetterStatus = Value
  val inHand, inserted, insertedConfirmed, placeHolder = Value
}


