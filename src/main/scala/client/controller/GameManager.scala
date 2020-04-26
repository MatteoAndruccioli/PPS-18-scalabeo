package client.controller

import model._

/** Classe che si occupa di tenere aggiornato il modello del tabellone lato client.
 *
 */
object GameManager {
  private var _myHand: Option[LettersHand] = Option.empty
  private var _board: Option[Board] = Option.empty

  /** Metodo chiamato quando si inizia una nuova partita.
   *
   * @param myHand la mano iniziale del giocatore
   */
  def newGame(myHand: Vector[Card]): Unit = {
    _board = Option(BoardImpl())
    _myHand = Option(LettersHandImpl(myHand))
  }

  /** Metodo chiamato per aagiornare le tessere nella mano del giocatore.
   *
   * @param cards le tessere del giocatore
   */
  def changeHand(cards: Vector[Card]): Unit = {
    _myHand.get.changeHand(cards)
  }

  /** Metodo utilizzato per aggiungere una tessera in una casella del tabellone.
   *
   * @param indexOfCard indice della carta giocata nella mano del giocatore
   * @param x riga del tabellone in cui si inserisce la tessera
   * @param y colonna del tabellone in cui si inserisce la tessera
   */
  def addCardToTile(indexOfCard: Int, x: Int, y: Int): Unit = {
    _board.get.addCard2Tile(_myHand.get.hand(indexOfCard), x, y, true)
  }

  /** Metodo che fa ritornare le lettere del giocatore posizionate in questo turno sul tabellone ma non ancora
   * confermate nella sua mano.
   *
   */
  def collectLetters(): Unit = {
    _board.get.clearBoardFromPlayedWords()
    _board.get.clearPlayedWords()
  }

  /** Metodo che ritorna la parola giocata in questo turno.
   *
   * @return una lista di BoardTile che rappresenta la sequenza di lettere che il giocatore ha giocato questo turno
   */
  def getPlayedWord: List[BoardTile] = {
    _board.get.playedWord
  }

  /** Metodo che inserisce definitivamente la parola nel tabellone senza possibilità di spostare più le lettere inserite.
   *
   * @param word la lettera da inserire.
   */
  def addPlayedWordAndConfirm(word: List[BoardTile]): Unit = {
    _board.get.addPlayedWord(word)
    _board.get.clearPlayedWords()
  }

  /** Stabilisce se il giocatore ha la possibilità di ripescare le lettere. Questo avviene quando il giocatore ha nella
   * sua mano solo consonanti o solo vocali.
   *
   * @return se la condizione è soddisfatta o meno
   */
  def isMulliganAvailable(): Boolean = {
    if(_myHand != Option.empty) {
      _myHand.get.containsOnlyVowelsOrOnlyConsonants()
    } else {
      false
    }
  }

  /** Metodo chiamato quando inizia un nuovo turno del giocatore.
   *
   */
  def newTurn(): Unit = {
    _board.get.clearPlayedWords()
  }

  /** Metodo che conferma la parola giocata dal giocatore.
   *
   */
  def confirmPlay(): Unit = {
    _board.get.clearPlayedWords()
  }

  /** Metodo che ripristina la classe quando la partita finisce.
   *
   */
  def endGame(): Unit = {
    _myHand = Option.empty
    _board = Option.empty
  }
}
