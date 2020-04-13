package client.view

import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{ColumnConstraints, GridPane, HBox, RowConstraints}
import scalafx.scene.text.Text

class Legend extends GridPane {
  prefHeight = 480
  prefWidth = 250
  styleClass += "legend"
  val colInfo = new ColumnConstraints(125)
  val rowInfo = new RowConstraints(480 / 11)
  columnConstraints = List.fill(2)(colInfo)
  rowConstraints = List.fill(11)(rowInfo)
  List(('A', 1), ('B', 4), ('C', 1), ('D', 4),
  ('E', 1), ('F', 4), ('G', 4), ('H', 8),
  ('I', 1), ('L', 2), ('M', 2), ('N', 2),
  ('O', 1), ('P', 3), ('Q', 10), ('R', 1),
  ('S', 1),  ('T', 1), ('U', 4), ('V', 4), ('Z', 8))
  .zipWithIndex
  .foreach(l => add(new HBox() {
    styleClass += "legend-entry"
    children = new Text(l._1._1.toString + " = " + l._1._2.toString)
  }, if(l._1._1 <= 'M') 0 else 1, l._2 % 11))

  add(new HBox() {
    styleClass += "legend-entry"
    children = List(
      new ImageView(new Image("/assets/start.png")) {
        fitHeight = 30
        fitWidth = 30
      },
      new Text(" = 1")
    )
  }, 1, 10)
}
