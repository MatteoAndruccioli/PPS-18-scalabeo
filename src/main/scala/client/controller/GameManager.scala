package client.controller

import model._


object GameManager {
  private var _myHand: Option[LettersHand] = Option.empty
  private var _board: Option[Board] = Option.empty

  def newGame(myHand: Vector[Card]): Unit = {
    _board = Option(BoardImpl())
    _myHand = Option(LettersHandImpl(myHand))
  }

  def changeHand(cards: Vector[Card]): Unit = {
    _myHand.get.changeHand(cards)
  }

  def addCardToTile(indexOfCard: Int, x: Int, y: Int): Unit = {
    _board.get.addCard2Tile(_myHand.get.hand(indexOfCard), x, y, true)
  }

  def collectLetters(): Unit = {
    _board.get.clearBoardFromPlayedWords()
    _board.get.clearPlayedWords()
  }

  def getPlayedWord: List[BoardTile] = {
    _board.get.playedWord
  }

  def addPlayedWordAndConfirm(word: List[BoardTile]): Unit = {
    _board.get.addPlayedWord(word)
    _board.get.clearPlayedWords()
  }

  def isMulliganAvailable(): Boolean = {
    if(_myHand != Option.empty) {
      _myHand.get.containsOnlyVowelsOrOnlyConsonants()
    } else {
      false
    }
  }

  def newTurn(): Unit = {
    _board.get.clearPlayedWords()
  }

  def confirmPlay(): Unit = {
    _board.get.clearPlayedWords()
  }

  def endGame(): Unit = {
    _myHand = Option.empty
    _board = Option.empty
  }
}
