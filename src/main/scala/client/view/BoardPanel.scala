package client.view

import scalafx.scene.layout.{ColumnConstraints, GridPane, RowConstraints}

class BoardPanel extends GridPane {

  var board: Array[Array[BoardTile]] = Array.ofDim[BoardTile](17, 17)
  maxHeight = 17 * 32
  maxWidth = 17 * 32
  styleClass += "board-panel"
  val colInfo = new ColumnConstraints(minWidth = 32, prefWidth = 32, maxWidth = 32)
  val rowInfo = new RowConstraints(minHeight = 32, prefHeight = 32, maxHeight = 32)
  for(row <- 1 to 17) {
    columnConstraints.add(colInfo)
    rowConstraints.add(rowInfo)
    for(column <- 1 to 17) {
      val boardTile = new BoardTile(32, row, column)
      add(boardTile, row - 1, column - 1)
      board(row - 1)(column - 1) = boardTile
    }
  }

}