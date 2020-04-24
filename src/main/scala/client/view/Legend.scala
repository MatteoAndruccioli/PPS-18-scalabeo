package client.view

import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{ColumnConstraints, GridPane, HBox, RowConstraints}
import scalafx.scene.text.Text

/** Classe che rappresenta la legenda mostrata affianco al tabellone di gioco. Contiene per ogni lettera il suo valore
 * in termini di punteggio.
 *
 */
class Legend extends GridPane {
  private val WIDTH = 250
  private val HEIGHT = 480
  private val COLUMN_CONSTRAINTS = 125
  private val COLUMN_CONSTRAINTS_COUNT = 2
  private val ROW_CONSTRAINTS = 480
  private val ROW_CONSTRAINTS_COUNT = 11
  private val START_IMAGE_URL = "/assets/start.png"
  private val IMAGE_SIZE = 30
  prefWidth = WIDTH
  prefHeight = HEIGHT
  styleClass += "legend"
  val colInfo = new ColumnConstraints(COLUMN_CONSTRAINTS)
  val rowInfo = new RowConstraints(ROW_CONSTRAINTS / ROW_CONSTRAINTS_COUNT)
  columnConstraints = List.fill(COLUMN_CONSTRAINTS_COUNT)(colInfo)
  rowConstraints = List.fill(ROW_CONSTRAINTS_COUNT)(rowInfo)
  List(('A', 1), ('B', 4), ('C', 1), ('D', 4),
  ('E', 1), ('F', 4), ('G', 4), ('H', 8),
  ('I', 1), ('L', 2), ('M', 2), ('N', 2),
  ('O', 1), ('P', 3), ('Q', 10), ('R', 1),
  ('S', 1),  ('T', 1), ('U', 4), ('V', 4), ('Z', 8))
  .zipWithIndex
  .foreach(l => add(new HBox() {
    styleClass += "legend-entry"
    children = new Text(l._1._1.toString + " = " + l._1._2.toString)
  }, if(l._1._1 <= 'M') 0 else 1, l._2 % ROW_CONSTRAINTS_COUNT))

  add(new HBox() {
    styleClass += "legend-entry"
    children = List(
      new ImageView(new Image(START_IMAGE_URL)) {
        fitHeight = IMAGE_SIZE
        fitWidth = IMAGE_SIZE
      },
      new Text(" = 1")
    )
  }, 1, 10)
}
