package client.view

import scalafx.application.Platform
import scalafx.scene.layout.{ColumnConstraints, GridPane, RowConstraints}

class BoardPanel extends GridPane {

  val BOARD_SIZE = 17
  val TILE_SIZE = 32
  var board: Array[Array[BoardTile]] = Array.ofDim[BoardTile](BOARD_SIZE, BOARD_SIZE)

  styleClass += "board-panel"

  maxHeight = BOARD_SIZE * TILE_SIZE
  maxWidth = BOARD_SIZE * TILE_SIZE
  val colInfo = new ColumnConstraints(minWidth = TILE_SIZE, prefWidth = TILE_SIZE, maxWidth = TILE_SIZE)
  val rowInfo = new RowConstraints(minHeight = TILE_SIZE, prefHeight = TILE_SIZE, maxHeight = TILE_SIZE)

  for(row <- 1 to BOARD_SIZE) {
    columnConstraints.add(colInfo)
    rowConstraints.add(rowInfo)
    for(column <- 1 to BOARD_SIZE) {
      val boardTile = new BoardTile(TILE_SIZE, row, column)
      add(boardTile, row - 1, column - 1)
      board(row - 1)(column - 1) = boardTile
    }
  }

  def updateBoard(word: List[(LetterTile, Int, Int)]): Unit = {
    Platform.runLater(() => {
      word.foreach(l => board(l._2 - 1)(l._3 - 1).center = l._1)
    })
  }

}