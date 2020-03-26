package model

import org.scalatest._
import model.BoardImpl

class BoardTest extends FlatSpec {

  "A card " should " be added to the board in a specific position" in {
    val card = CardImpl("A")
    val board = BoardImpl()
    board.addCard2Tile(card, 1,1 )
    assert(board.boardTiles.head.card.equals(card))
  }

  "A card " should " be removed to the board in a specific position" in {
    val card = CardImpl("A")
    val board = BoardImpl()
    board.addCard2Tile(card, 1,1 )
    board.removeCardFromTile(1,1)
    assert(board.boardTiles.head.card.equals(boardConstants.defaultCard))
  }

}
