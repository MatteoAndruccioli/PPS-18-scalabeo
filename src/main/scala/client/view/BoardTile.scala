package client.view

import client.view.TileType.TileType
import scalafx.Includes.handle
import scalafx.scene.layout.BorderPane

/** Classe che modella le varie caselle del tabellone.
 *
 * @param size grandezza in pixel della casella
 * @param row posizione nella riga della casella
 * @param column posizione nella colonna della casella
 */
class BoardTile(size: Double, val row: Int, val column: Int) extends BorderPane {
  prefWidth = size
  prefHeight = size
  maxWidth = size
  maxHeight = size

  stylesheets_=(List("/style/TileStyle.css"))
  styleClass += "board-tile"

  val tileType: TileType = BoardTile.getTileType(row, column)
  val tileStyle: String = tileType match {
    case TileType.Base => ""
    case TileType.Start => "board-tile-start"
    case TileType.ThreeXLetter => "board-tile-3letter"
    case TileType.TwoXLetter => "board-tile-2letter"
    case TileType.TwoXWord => "board-tile-2word"
    case TileType.ThreeXWord => "board-tile-3word"
    case _ => ""
  }

  if(tileStyle != "") {
    styleClass += tileStyle
  }

  onMousePressed = handle {
    BoardInteraction.insert(this)
  }
}

/** Oggetto che associa ad ogni coppia di valori riga e colonna un tipo di casella.
 *
 */
object BoardTile {
  var TwoXLettersTile: List[(Int, Int)] = List((1, 5), (1, 13), (3, 8), (3, 10), (4, 9),
    (5, 1), (5, 17), (8, 3),(8, 8), (8, 10), (8, 15), (9, 4), (9, 14), (10, 3), (10, 8),
    (10, 10), (10, 15), (13, 1), (13, 17), (14, 9), (15, 8), (15, 10), (17, 5),(17,13))
  var ThreeXLettersTile: List[(Int, Int)] = List((2, 7), (2, 11), (7, 2), (7, 7), (7, 11),
    (7, 16), (11, 2), (11, 7), (11, 11), (11, 16), (16, 7), (16, 11))
  var ThreeXWordTile: List[(Int, Int)] = List((1, 1), (9, 1), (17, 1), (1, 9), (17, 9),
    (1, 17), (9, 17), (17, 17))
  var TwoXWordTile: List[(Int, Int)] = List((2, 2), (3, 3), (4, 4), (5, 5), (6, 6),
    (12, 12), (13, 13), (14, 14), (15, 15), (16, 16), (2, 16), (3, 15), (4, 14),
    (5, 13), (6, 12), (12, 6), (13, 5), (14, 4), (15, 3), (16, 2))
  var StartTile: List[(Int, Int)] = List((9, 9))

  /** Metodo usato per stabilire il tipo di casella a partire dall'indice della riga e della colonna.
   *
   * @param row indice della riga
   * @param column indice della colonna
   * @return il tipo della casella
   */
  def getTileType(row: Int, column: Int): TileType = (row, column) match {
    case _ if TwoXLettersTile.contains((row, column)) => TileType.TwoXLetter
    case _ if ThreeXLettersTile.contains((row, column)) => TileType.ThreeXLetter
    case _ if ThreeXWordTile.contains((row, column)) => TileType.ThreeXWord
    case _ if TwoXWordTile.contains((row, column)) => TileType.TwoXWord
    case _ if StartTile.contains((row, column)) => TileType.Start
    case _ => TileType.Base
  }
}

/** Enumerazione che contiene i vari tipi di caselle presenti sul tabellone.
 *
 */
object TileType extends Enumeration {
  type TileType = Value
  val TwoXLetter, ThreeXLetter, TwoXWord, ThreeXWord, Start, Base = Value
}
