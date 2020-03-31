package model

import org.scalatest._
import scala.collection.mutable.ArrayBuffer

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
    board.addCard2Tile(card, 1,1)
    board.removeCardFromTile(1,1)
    assert(board.boardTiles.head.card.equals(boardConstants.defaultCard))
  }

  "A card " should " be added to the board in a specific position and in played word" in {
    val card = CardImpl("A")
    val board = BoardImpl()
    board.addCard2Tile(card, 1,1, add2PlayedWord = true)
    assert(board.boardTiles.head.card.equals(card) && board.playedWord.contains(BoardTileImpl(Position(1,1),card)))
  }

  "A card " should " be removed to the board in a specific position and from played word" in {
    val card = CardImpl("A")
    val board = BoardImpl()
    board.addCard2Tile(card, 1,1)
    board.removeCardFromTile(1,1, removeFromPlayedWord = true)
    assert(board.boardTiles.head.card.equals(boardConstants.defaultCard) && !board.playedWord.contains(BoardTileImpl(Position(1,1),card)))
  }

  "A hand " should " be removed add to board" in {
    val card = CardImpl("A")
    val board = BoardImpl()
    board.addCard2Tile(card, 1,1)
    board.removeCardFromTile(1,1, removeFromPlayedWord = true)
    assert(board.boardTiles.head.card.equals(boardConstants.defaultCard) && !board.playedWord.contains(BoardTileImpl(Position(1,1),card)))
  }

  "A list of Card " should " be added to the board and removed" in {
    val boardTile = BoardTileImpl(new Position(1,3), CardImpl("D"))
    val boardTile1 = BoardTileImpl(new Position(2,3), CardImpl("B"))
    val boardTile2 = BoardTileImpl(new Position(3,3), CardImpl("E"))
    val listBoardTile = List(boardTile,boardTile1,boardTile2)
    val board = BoardImpl()
    board.addPlayedWord(listBoardTile)
    assert(board.playedWord.equals(listBoardTile))
    board.clearPlayedWords()
    assert(board.playedWord.equals(List()))
  }

  "A list of card played " should " be remove from board" in {
    val boardTile = BoardTileImpl(new Position(1,3), CardImpl("D"))
    val boardTile1 = BoardTileImpl(new Position(2,3), CardImpl("B"))
    val boardTile2 = BoardTileImpl(new Position(3,3), CardImpl("E"))
    val listBoardTile = List(boardTile,boardTile1,boardTile2)
    val board = BoardImpl()
    board.addPlayedWord(listBoardTile)
    board.clearBoardFromPlayedWords()
    assert(!board.boardTiles.contains(listBoardTile))
  }

  "A list of card played " should " be insert in the same row or in the same column" in {
    val card = CardImpl("A")
    val boardTile = BoardTileImpl(new Position(1,3), card)
    val boardTile1 = BoardTileImpl(new Position(1,2), card)
    val listBoardTile = List(boardTile,boardTile1)
    val board = BoardImpl()
    board.addPlayedWord(listBoardTile)
    assert(board.checkGoodWordDirection())
    board.clearBoardFromPlayedWords()
    val boardTile2 = BoardTileImpl(new Position(1,1), card)
    val boardTile3 = BoardTileImpl(new Position(2,1), card)
    val listBoardTile2 = List(boardTile2,boardTile3)
    board.addPlayedWord(listBoardTile2)
    assert(board.checkGoodWordDirection())
    val boardTile4 = BoardTileImpl(new Position(1,1), card)
    val boardTile5 = BoardTileImpl(new Position(2,2), card)
    val listBoardTile3 = List(boardTile4,boardTile5)
    board.addPlayedWord(listBoardTile3)
    assert(!board.checkGoodWordDirection())
  }

  "The list of check words" should "be the cross of the card that is played" in {
    val board = BoardImpl()
    val boardTileE = BoardTileImpl(new Position(2,3), CardImpl("E"))
    val listOfWords = List(ArrayBuffer((CardImpl("B"),"DEFAULT"), (CardImpl("E"),"DEFAULT"), (CardImpl("G"),"2P")), ArrayBuffer((CardImpl("D"),"2P"), (CardImpl("E"),"DEFAULT"), (CardImpl("F"),"DEFAULT")))
    board.addCard2Tile(CardImpl("B"), 1, 3)
    board.addCard2Tile(CardImpl("D"), 2, 2)
    board.addCard2Tile(CardImpl("F"), 2, 4)
    board.addCard2Tile(CardImpl("G"), 3,3)
    board.addPlayedWord(List(boardTileE))
    assert(board.takeCardToCalculatePoints().equals(listOfWords))
  }

  "The list of check words from vertical cards" should "contain also the horizontal crossings" in {
    val board = BoardImpl()
    val boardTileE = BoardTileImpl(new Position(2,3), CardImpl("E"))
    val boardTileB = BoardTileImpl(new Position(1,3), CardImpl("B"))
    val listOfWords = List(ArrayBuffer((CardImpl("D"),"2P"), (CardImpl("E"),"DEFAULT"), (CardImpl("F"),"DEFAULT")), ArrayBuffer((CardImpl("B"),"DEFAULT"), (CardImpl("E"),"DEFAULT")), ArrayBuffer((CardImpl("A"),"DEFAULT"), (CardImpl("B"),"DEFAULT"), (CardImpl("C"),"DEFAULT")))
    board.addCard2Tile(CardImpl("A"), 1, 2)
    board.addCard2Tile(CardImpl("D"), 2, 2)
    board.addCard2Tile(CardImpl("C"), 1,4)
    board.addCard2Tile(CardImpl("F"), 2,4)
    board.addPlayedWord(List(boardTileB,boardTileE))
    assert(board.takeCardToCalculatePoints().equals(listOfWords))
  }

  "The list of check words from horizontal cards" should "contain also the vertical crossings" in {
    val board = BoardImpl()
    val boardTileE = BoardTileImpl(new Position(2,2), CardImpl("C"))
    val boardTileB = BoardTileImpl(new Position(2,3), CardImpl("D"))
    val listOfWords = List(ArrayBuffer((CardImpl("A"),"DEFAULT"), (CardImpl("C"),"2P"), (CardImpl("E"),"DEFAULT")), ArrayBuffer((CardImpl("B"),"DEFAULT"), (CardImpl("D"),"DEFAULT"), (CardImpl("F"),"2P")), ArrayBuffer((CardImpl("C"),"2P"), (CardImpl("D"),"DEFAULT")))
    board.addCard2Tile(CardImpl("A"), 1, 2)
    board.addCard2Tile(CardImpl("B"), 1, 3)
    board.addCard2Tile(CardImpl("E"), 3,2)
    board.addCard2Tile(CardImpl("F"), 3,3)
    board.addPlayedWord(List(boardTileB,boardTileE))
    assert(board.takeCardToCalculatePoints().equals(listOfWords))
  }

  "The word" should "be extracted from the list of Card" in {
    val board = BoardImpl()
    val listOfWords: List[ArrayBuffer[(Card,String)]] = List(ArrayBuffer((CardImpl("O"),"DEFAULT"), (CardImpl("C"),"2P"), (CardImpl("A"),"DEFAULT")))
    assert(board.getWordsFromLetters(listOfWords).contains("oca"))
  }

}
