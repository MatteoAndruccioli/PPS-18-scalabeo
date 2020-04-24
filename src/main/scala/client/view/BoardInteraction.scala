package client.view

import client.controller.Controller
import javafx.scene.layout.Pane
import scalafx.application.Platform
import scalafx.scene.layout.HBox

import scala.collection.mutable

/** BoardInteraction è un oggetto che si occupa di aggiornare graficamente il tabellone e la mano del giocatore.
 *
 */
object BoardInteraction {

  private val LETTER_SIZE = 60
  private var _selection: LetterTile = _
  private var _handHBox: Pane = _
  private val _thisTurnInsertions: mutable.Map[LetterTile, (Int, Int)] = mutable.Map()

  /** Metodo chiamato per mostrare all'utente che ha premuto su una lettera e che questa è stata selezionata, la lettera
   * viene traslata sull'asse verticale.
   *
   * @param letterTile la lettera che è stata selezionata
   */
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

  /** Metodo che inserisce sul tabellone una lettera che in precedenza si trovava nella mano del giocatore.
   *
   * @param boardTile la casella di destinazione della lettera
   */
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

  /** Metodo che fa ritornare le lettere del giocatore posizionate in questo turno sul tabellone ma non ancora
   * confermate nella sua mano.
   *
   */
  def collectLetters(): Unit = {
    _thisTurnInsertions.keys.foreach(letter => Platform.runLater(() => {
      _handHBox.getChildren.add(letter)
      _thisTurnInsertions -= letter
      letter.letterStatus = LetterStatus.inHand
      letter.unselect()
    }))
    _thisTurnInsertions.clear()
  }

  /** Metodo chiamato quando la parola giocata dall'utente è stata accetata, di conseguenza non è più possibile spostare
   * le lettere che il giocatore ha posizionato in qeusto turno.
   *
   */
  def confirmPlay(): Unit = {
    _thisTurnInsertions.keys.foreach(letter => {
      letter.letterStatus = LetterStatus.insertedConfirmed
      _thisTurnInsertions -= letter
      letter.unselect()
    })
    _thisTurnInsertions.clear()
    _selection = null
  }

  /** Metodo chiamato per impostare il contenitore delle tessere del giocatore.
   *
   * @param myHand il contenitore in cui sono presenti le tessere del giocatore
   */
  def setMyHand(myHand: HBox): Unit = {
    _handHBox = myHand
  }

  /** Metodo che aggiorna la mano del giocatore in seguito alla pescata di nuove lettere.
   *
   * @param cards le carte del giocatore in questo turno.
   */
  def updateHand(cards: Vector[(String, Int)]): Unit = {
    Platform.runLater(() => {
      _handHBox.getChildren.clear()
      cards.zipWithIndex.foreach(c => _handHBox.getChildren.add(LetterTile(LETTER_SIZE, c._1._1, c._1._2.toString, c._2, LetterStatus.inHand)))
    })
  }

  /** Metodo utilizzato per resettare questa classe a fine partita.
   *
   */
  def reset(): Unit = {
    this._thisTurnInsertions.empty
  }

}
