package client.view

import client.controller.Controller
import javafx.scene.layout.Pane
import scalafx.application.Platform
import scalafx.scene.layout.HBox

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object BoardInteraction {

  private val LETTER_SIZE = 60
  private var _selection: LetterTile = _
  private var _handHBox: Pane = _
  private val _thisTurnInsertions: mutable.Map[LetterTile, (Int, Int)] = mutable.Map()

  def select(letterTile: LetterTile): Unit = {
    if(Controller.isMyTurn && letterTile.letterStatus == LetterStatus.inHand) {
      if(letterTile == _selection) {
        _selection = null
        letterTile.unselect()
      } else {
        if(this._selection != null) {
          this._selection.unselect()
        }
        this._selection = letterTile
      }
    }
  }

  def insert(boardTile: BoardTile): Unit = {
    if(Controller.isMyTurn && _selection != null && boardTile.getChildren.isEmpty) {
      _selection.getParent.asInstanceOf[Pane].getChildren.remove(_selection)
      _thisTurnInsertions.put(_selection, (boardTile.row, boardTile.column))
      boardTile.center = _selection
      _selection.letterStatus = LetterStatus.inserted
      Controller.addCardToTile(_selection.position, boardTile.row, boardTile.column)
      _selection.unselect()
      _selection = null
    }
  }

  def collectLetters(): Unit = {
    _thisTurnInsertions.keys.foreach(letter => Platform.runLater(() => {
      _handHBox.getChildren.add(letter)
      _thisTurnInsertions -= letter
      letter.letterStatus = LetterStatus.inHand
      letter.unselect()
    }))
    _thisTurnInsertions.clear()
  }

  def confirmPlay(): Unit = {
    _thisTurnInsertions.keys.foreach(letter => {
      letter.letterStatus = LetterStatus.insertedConfirmed
      _thisTurnInsertions -= letter
      letter.unselect()
    })
    _thisTurnInsertions.clear()
    _selection = null
  }

  def setMyHand(myHand: HBox): Unit = {
    _handHBox = myHand
  }

  def updateHand(cards: ArrayBuffer[(String, Int)]): Unit = {
    Platform.runLater(() => {
      _handHBox.getChildren.clear()
      cards.zipWithIndex.foreach(c => _handHBox.getChildren.add(LetterTile(LETTER_SIZE, c._1._1, c._1._2.toString, c._2, LetterStatus.inHand)))
    })
  }

  def reset(): Unit = {
    this._thisTurnInsertions.empty
  }

}
