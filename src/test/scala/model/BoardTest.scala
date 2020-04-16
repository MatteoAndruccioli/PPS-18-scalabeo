package model

import org.scalatest._
import scala.collection.mutable.ArrayBuffer

class BoardTest extends FlatSpec {

  // TEST SUI METODI UTILIZZATI PER INSERIRE E RIMUOVERE ELEMENTI DA BOARD
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
    assert(board.boardTiles.head.card.equals(constants.defaultCard))
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
    assert(board.boardTiles.head.card.equals(constants.defaultCard) && !board.playedWord.contains(BoardTileImpl(Position(1,1),card)))
  }
  "A hand " should " be removed add to board" in {
    val card = CardImpl("A")
    val board = BoardImpl()
    board.addCard2Tile(card, 1,1)
    board.removeCardFromTile(1,1, removeFromPlayedWord = true)
    assert(board.boardTiles.head.card.equals(constants.defaultCard) && !board.playedWord.contains(BoardTileImpl(Position(1,1),card)))
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
    assert(board.playedWord.isEmpty)
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


  // TEST SUI CONTROLLI DELLA PRIMA PAROLA INSERITA
  "The first word" should "be on SCARABEO image" in {
    val board = BoardImpl()
    val boardTile = BoardTileImpl(new Position(9,9), CardImpl("F"))
    val boardTile1 = BoardTileImpl(new Position(10,9), CardImpl("I"))
    board.addPlayedWord(List(boardTile,boardTile1))
    assert(board.checkGameFirstWord())
  }
  "The first letters played" should "be adjacent " in {
    val board = BoardImpl()
    val boardTile = BoardTileImpl(new Position(9,9), CardImpl("F"))
    val boardTile1 = BoardTileImpl(new Position(10,9), CardImpl("I"))
    val boardTile2 = BoardTileImpl(new Position(11,9), CardImpl("C"))
    board.addPlayedWord(List(boardTile,boardTile1, boardTile2))
    assert(board.checkGameFirstWord())
    board.clearPlayedWords()
    val boardTile3 = BoardTileImpl(new Position(9,9), CardImpl("F"))
    val boardTile4 = BoardTileImpl(new Position(9,10), CardImpl("I"))
    val boardTile5 = BoardTileImpl(new Position(9,11), CardImpl("C"))
    board.addPlayedWord(List(boardTile3,boardTile4, boardTile5))
    assert(board.checkGameFirstWord())
  }
  "The first letters played" should "be adjacent (played not in order)" in {
    val board = BoardImpl()
    val boardTile = BoardTileImpl(new Position(11,9), CardImpl("F"))
    val boardTile1 = BoardTileImpl(new Position(9,9), CardImpl("I"))
    val boardTile2 = BoardTileImpl(new Position(10,9), CardImpl("C"))
    board.addPlayedWord(List(boardTile,boardTile1, boardTile2))
    assert(board.checkGameFirstWord())
    board.clearPlayedWords()
    val boardTile3 = BoardTileImpl(new Position(9,11), CardImpl("F"))
    val boardTile4 = BoardTileImpl(new Position(9,9), CardImpl("I"))
    val boardTile5 = BoardTileImpl(new Position(9,10), CardImpl("C"))
    board.addPlayedWord(List(boardTile3,boardTile4, boardTile5))
    assert(board.checkGameFirstWord())
  }
  "The first letters played not adjacent" should "be found " in {
    val board = BoardImpl()
    val boardTile = BoardTileImpl(new Position(9,9), CardImpl("F"))
    val boardTile1 = BoardTileImpl(new Position(10,9), CardImpl("I"))
    val boardTile2 = BoardTileImpl(new Position(15,9), CardImpl("C"))
    board.addPlayedWord(List(boardTile,boardTile1, boardTile2))
    assert(!board.checkGameFirstWord())
    board.clearPlayedWords()
    val boardTile3 = BoardTileImpl(new Position(9,9), CardImpl("F"))
    val boardTile4 = BoardTileImpl(new Position(9,10), CardImpl("I"))
    val boardTile5 = BoardTileImpl(new Position(9,13), CardImpl("C"))
    board.addPlayedWord(List(boardTile3,boardTile4, boardTile5))
    assert(!board.checkGameFirstWord())
  }
  "The first letters played not adjacent" should "be found: case 2" in {
    val board = BoardImpl()
    val boardTile = BoardTileImpl(new Position(9,9), CardImpl("F"))
    val boardTile1 = BoardTileImpl(new Position(10,10), CardImpl("I"))
    val boardTile2 = BoardTileImpl(new Position(11,11), CardImpl("C"))
    board.addPlayedWord(List(boardTile,boardTile1, boardTile2))
    assert(!board.checkGameFirstWord())
  }

  // TEST PER IL CONTROLLO DELLE PAROLE ESTRATTE DALLA BOARD
  "A list of card played " should " be insert in the same row or in the same column" in {
    val card = CardImpl("A")
    val boardTile = BoardTileImpl(new Position(1,3), card)
    val boardTile1 = BoardTileImpl(new Position(1,2), card)
    val listBoardTile = List(boardTile,boardTile1)
    val board = BoardImpl()
    board.addPlayedWord(listBoardTile)
    assert(board.takeCardToCalculatePoints().nonEmpty)
    board.clearBoardFromPlayedWords()
    val boardTile2 = BoardTileImpl(new Position(1,1), card)
    val boardTile3 = BoardTileImpl(new Position(2,1), card)
    val listBoardTile2 = List(boardTile2,boardTile3)
    board.addPlayedWord(listBoardTile2)
    assert(board.takeCardToCalculatePoints().nonEmpty)
    val boardTile4 = BoardTileImpl(new Position(1,1), card)
    val boardTile5 = BoardTileImpl(new Position(2,2), card)
    val listBoardTile3 = List(boardTile4,boardTile5)
    board.addPlayedWord(listBoardTile3)
    assert(board.takeCardToCalculatePoints().isEmpty)
  }
  "The letters played not " should "adjacent to the letters in the Board" in {
    val board = BoardImpl()
    val boardTile = BoardTileImpl(new Position(9,9), CardImpl("F"))
    val boardTile1 = BoardTileImpl(new Position(9,10), CardImpl("I"))
    board.addPlayedWord(List(boardTile,boardTile1))
    assert(board.takeCardToCalculatePoints().nonEmpty)
    board.addPlayedWord(List(BoardTileImpl(new Position(1,2), CardImpl("I"))))
    assert(board.takeCardToCalculatePoints().isEmpty)
  }
  "Not adjacent letters" should "not make a word" in {
    val board = BoardImpl()
    val boardTileC = BoardTileImpl(new Position(3,1), CardImpl("C"))
    val boardTileD = BoardTileImpl(new Position(3,4), CardImpl("D"))
    board.addCard2Tile(CardImpl("A"), 2, 3)
    board.addCard2Tile(CardImpl("B"), 3, 3)
    board.addPlayedWord(List(boardTileC,boardTileD))
    assert(board.takeCardToCalculatePoints().isEmpty)
  }
  "Not adjacent letters" should "not make a word (vertical)" in {
    val board = BoardImpl()
    val boardTileC = BoardTileImpl(new Position(2,3), CardImpl("S"))
    val boardTileD = BoardTileImpl(new Position(5,3), CardImpl("I"))
    board.addCard2Tile(CardImpl("S"), 3, 2)
    board.addCard2Tile(CardImpl("i"), 3, 3)
    board.addPlayedWord(List(boardTileC,boardTileD))
    assert(board.takeCardToCalculatePoints().isEmpty)
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

  // TEST PER RICAVARE LA PAROLA NEL FORMATO DEL DIZIONARIO DALLE LETTERE GIOCATE
  "The word" should "be extracted from the list of Card" in {
    val board = BoardImpl()
    val listOfWords: List[ArrayBuffer[(Card,String)]] = List(ArrayBuffer((CardImpl("O"),"DEFAULT"), (CardImpl("C"),"2P"), (CardImpl("A"),"DEFAULT")))
    assert(board.getWordsFromLetters(listOfWords).contains("oca"))
  }

  // TEST PER IL CALCOLO DEL PUNTEGGIO DELLE PAROLE
  "The word points" should "respect the rules of Scarabeo" in {
    val board = BoardImpl()
    val aspectedPoints = 28
    val boardTile = BoardTileImpl(new Position(1,2), CardImpl("F"))
    val boardTile1 = BoardTileImpl(new Position(2,2), CardImpl("I"))
    val boardTile2 = BoardTileImpl(new Position(3,2), CardImpl("C"))
    val boardTile3 = BoardTileImpl(new Position(4,2), CardImpl("O"))
    board.addPlayedWord(List(boardTile,boardTile1,boardTile2, boardTile3))
    assert(board.calculateTurnPoints(board.takeCardToCalculatePoints()) == aspectedPoints)
  }
  "The word points" should "be doubled for the first word" in {
    val board = BoardImpl()
    val aspectedPoints = 14
    val boardTile = BoardTileImpl(new Position(1,2), CardImpl("F"))
    val boardTile1 = BoardTileImpl(new Position(2,2), CardImpl("I"))
    val boardTile2 = BoardTileImpl(new Position(3,2), CardImpl("C"))
    val boardTile3 = BoardTileImpl(new Position(4,2), CardImpl("O"))
    board.addPlayedWord(List(boardTile,boardTile1,boardTile2, boardTile3))
    // calcolo punteggio prima parola
    assert(board.calculateTurnPoints(board.takeCardToCalculatePoints()) == aspectedPoints*scoreConstants.firstWordBonus)
    // calcolo punteggio seconda parola
    assert(board.calculateTurnPoints(board.takeCardToCalculatePoints()) == aspectedPoints)
  }
  "The word 'SCARABEO'" should "have a bonus" in {
    val board = BoardImpl()
    val aspectedPoints = 112
    val boardTile = BoardTileImpl(new Position(1,2), CardImpl("S"))
    val boardTile1 = BoardTileImpl(new Position(2,2), CardImpl("C"))
    val boardTile2 = BoardTileImpl(new Position(3,2), CardImpl("A"))
    val boardTile3 = BoardTileImpl(new Position(4,2), CardImpl("R"))
    val boardTile4 = BoardTileImpl(new Position(5,2), CardImpl("A"))
    val boardTile5 = BoardTileImpl(new Position(6,2), CardImpl("B"))
    val boardTile6 = BoardTileImpl(new Position(7,2), CardImpl("E"))
    val boardTile7 = BoardTileImpl(new Position(8,2), CardImpl("O"))
    board.addPlayedWord(List(boardTile,boardTile1,boardTile2, boardTile3, boardTile4,boardTile5,boardTile6, boardTile7))
    assert(board.calculateTurnPoints(board.takeCardToCalculatePoints()) == aspectedPoints+scoreConstants.bonusScarabeoWord)
  }
}
