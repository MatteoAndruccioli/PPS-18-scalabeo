package model

import scala.collection.mutable.ArrayBuffer

package object boardConstants{
  val boardBonus: Map[(Int, Int), String] = Map((1, 5) -> constants.letterForTwo, (1, 13) -> constants.letterForTwo, (3, 8) -> constants.letterForTwo,
    (3, 10) -> constants.letterForTwo, (4, 9) -> constants.letterForTwo, (5, 1) ->constants.letterForTwo,
    (5, 17) ->constants.letterForTwo, (8, 3) -> constants.letterForTwo,(8, 8) ->constants.letterForTwo,
    (8, 10) ->constants.letterForTwo, (8, 15) ->constants.letterForTwo, (9, 4) ->constants.letterForTwo,
    (9, 14) ->constants.letterForTwo, (10, 3) ->constants.letterForTwo, (10, 8) ->constants.letterForTwo,
    (10, 10) ->constants.letterForTwo, (10, 15) ->constants.letterForTwo, (13, 1) ->constants.letterForTwo,
    (13, 17) ->constants.letterForTwo, (14, 9) ->constants.letterForTwo, (15, 8) ->constants.letterForTwo,
    (15, 10) ->constants.letterForTwo, (17, 5) ->constants.letterForTwo,(17,13) ->constants.letterForTwo,
    (2, 7) ->constants.letterForThree, (2, 11) ->constants.letterForThree, (7, 2) ->constants.letterForThree,
    (7, 7) ->constants.letterForThree, (7, 11) ->constants.letterForThree, (7, 16) ->constants.letterForThree,
    (11, 2) ->constants.letterForThree, (11, 7) ->constants.letterForThree, (11, 11) ->constants.letterForThree,
    (11, 16) ->constants.letterForThree, (16, 7) ->constants.letterForThree, (16, 11) ->constants.letterForThree,
    (1, 1) ->constants.wordForThree, (9, 1) ->constants.wordForThree, (17, 1) ->constants.wordForThree,
    (1, 9) ->constants.wordForThree, (17, 9) ->constants.wordForThree, (1, 17) ->constants.wordForThree,
    (9, 17) ->constants.wordForThree, (17, 17) ->constants.wordForThree,
    (2, 2) ->constants.wordForTwo, (3, 3) ->constants.wordForTwo, (4, 4) ->constants.wordForTwo,
    (5, 5) ->constants.wordForTwo, (6, 6) ->constants.wordForTwo, (12, 12) ->constants.wordForTwo,
    (13, 13) ->constants.wordForTwo, (14, 14) ->constants.wordForTwo, (15, 15) ->constants.wordForTwo,
    (16, 16) ->constants.wordForTwo, (2, 16) ->constants.wordForTwo, (3, 15) ->constants.wordForTwo,
    (4, 14) ->constants.wordForTwo, (5, 13) ->constants.wordForTwo, (6, 12) ->constants.wordForTwo,
    (12, 6) ->constants.wordForTwo, (13, 5) ->constants.wordForTwo, (14, 4) ->constants.wordForTwo,
    (15, 3) ->constants.wordForTwo, (16, 2) ->constants.wordForTwo)

  val defaultCard = CardImpl("NULL")
  val boardTileDefault = BoardTileImpl(Position(-1, -1), defaultCard)

  val horizontal  = "H"
  val vertical = "V"
  val diagonal = "D"
}

// BoardTile: casella all'interno del tabellone
// fa riferimento ad una posizione e poi, quando viene aggiunta, ad una Card
sealed trait BoardTile{
  def card: Card
  def position: Position
}

case class BoardTileImpl(_position: Position, _card: Card) extends BoardTile{
  override def position: Position  = _position
  override def card: Card = _card
}

// Board: implementazione del modello del tabellone
sealed trait Board{
  def boardTiles: List[BoardTile]
  def playedWord: List[BoardTile]
  def addCard2Tile (card: Card, x:Int, y:Int, player:Boolean)
  def removeCardFromTile(x:Int, y:Int, player:Boolean): Card
  def addPlayedWord(playedWordsList: List[BoardTile])
  def clearPlayedWords()
  def clearBoardFromPlayedWords()
  def checkGoodWordDirection(): Boolean
  def takeCardToCalculatePoints():  List[ArrayBuffer[(Card, String)]]
  def getWordsFromLetters(word: List[ArrayBuffer[(Card, String)]]): List[String]
  def calculateTurnPoints(words: List[ArrayBuffer[(Card, String)]]): Int
  def playedWordIsOnScarabeo(): Boolean
  def lettersAreAdjacent(): Boolean

}

case class BoardImpl() extends Board {
  private var _boardTiles: List[BoardTile] = populateBoard()
  private var _playedWord: List[BoardTile] = List()
  private var _firstWord: Boolean = true

  override def boardTiles: List[BoardTile] = _boardTiles
  override def playedWord: List[BoardTile] = _playedWord

  private def populateBoard() = (for( x <- 1 to 17; y <- 1 to 17) yield tuple2BoardTile(x, y)).toList
  private def tuple2BoardTile(tuple: (Int, Int)): BoardTile = BoardTileImpl(Position.apply(tuple._1, tuple._2), boardConstants.defaultCard)

  private def samePosition(position: Position, x: Int, y: Int): Boolean = position.coord._1+1 == x && position.coord._2+1 == y
  private def getTileInAPosition(x:Int, y:Int): BoardTile = _boardTiles.find( boardTile => samePosition(boardTile.position,x, y)).getOrElse(boardConstants.boardTileDefault)

  // metodo per aggiungere una card in una posizione del tabellone
  override def addCard2Tile(card: Card, x:Int, y:Int, add2PlayedWord:Boolean = true): Unit =  {
    _boardTiles = _boardTiles.map {
      element => if (element.equals(getTileInAPosition(x, y))){
        if(add2PlayedWord) _playedWord = BoardTileImpl(new Position(x, y), card) :: _playedWord
        BoardTileImpl(new Position(x, y), card)}
      else element
    }
  }

  // metodo per rimuovere una card in una posizione del tabellone
  override def removeCardFromTile(x: Int, y: Int, removeFromPlayedWord:Boolean = true): Card = {
    var card: Card = boardConstants.defaultCard
    _boardTiles = _boardTiles.map {
      element => if (element.equals(getTileInAPosition(x, y))) {
        card = element.card
        if(removeFromPlayedWord) _playedWord = _playedWord.filter(boardTile => !boardTile.equals(element))
        BoardTileImpl(new Position(x, y), boardConstants.defaultCard)
      }else element
    }
    card
  }

  // metodi per aggiungere e rimuovere una lista di carte
  override def addPlayedWord(playedWordsList: List[BoardTile]): Unit = {
    _playedWord = List()
    _playedWord = _playedWord ++ playedWordsList
    for(playedWord <- playedWordsList)  addCard2Tile(playedWord.card, playedWord.position.coord._1+1, playedWord.position.coord._2+1, add2PlayedWord = false)
  }
  override def clearPlayedWords(): Unit = _playedWord = List()
  override def clearBoardFromPlayedWords(): Unit = for(playedWord <- _playedWord) removeCardFromTile(playedWord.position.coord._1+1, playedWord.position.coord._2+1, removeFromPlayedWord = false)

  // metodo per controllare che lettere siano in prizzontale o in verticale
  def checkGoodWordDirection(): Boolean = !(wordDirection(_playedWord) == boardConstants.diagonal)

  // metodo per controllare che le lettere inserite siano tutte nella stessa direzione
  private def wordDirection(wordList: List[BoardTile]): String = {
    if (wordList.forall(boardTiles => boardTiles.position.coord._1 == wordList.head.position.coord._1))
      boardConstants.vertical
    else if (wordList.forall(boardTiles => boardTiles.position.coord._2 == wordList.head.position.coord._2))
      boardConstants.horizontal
    else
      boardConstants.diagonal
  }

  // metodo per ottenere parole formate con le card inserite dall'utente in un turno
  override def takeCardToCalculatePoints(): List[ArrayBuffer[(Card, String)]] = {
    var listOfWords: List[ArrayBuffer[(Card, String)]] = List()

    // per la testa della lista controllo in tutti e due i versi
    listOfWords = (tileBoardsInADirection(Directions.N, _playedWord.head)++ ArrayBuffer(boardTails2Tuple(_playedWord.head)) ++ tileBoardsInADirection(Directions.S, _playedWord.head)) :: listOfWords
    listOfWords = (tileBoardsInADirection(Directions.W, _playedWord.head)++ ArrayBuffer(boardTails2Tuple(_playedWord.head)) ++ tileBoardsInADirection(Directions.E, _playedWord.head)) :: listOfWords
    // se le carte giocate sono di più allora devo cercare anche le parole formate dagli incroci
    if (_playedWord.length > 1) {
      if (wordDirection(_playedWord) == boardConstants.horizontal) {
        // le carte messe sono in orizzontale
        // cerco gli incroci in S/N (verticale)
        for(boardTile <- _playedWord.tail){
          listOfWords = (tileBoardsInADirection(Directions.N, boardTile)++ ArrayBuffer(boardTails2Tuple(boardTile)) ++ tileBoardsInADirection(Directions.S, boardTile)) :: listOfWords
        }
      } else if (wordDirection(_playedWord) == boardConstants.vertical) {
        // le carte messe sono in verticale
        // cerco gli incroci in W/E (orizzontale)
        for(boardTile <- _playedWord.tail) {
          listOfWords = (tileBoardsInADirection(Directions.W, boardTile) ++ ArrayBuffer(boardTails2Tuple(boardTile)) ++ tileBoardsInADirection(Directions.E, boardTile)) :: listOfWords
        }
      }
    }
    listOfWords.filter(array => array.length > 1)
  }

  // metodo per ottenere da una data posizione le carte inserite in una direzione
  private def tileBoardsInADirection(direction: Direction, boardTile: BoardTile): ArrayBuffer[(Card, String)] = {
    var wordReturn: ArrayBuffer[(Card,String)] = ArrayBuffer()
    var actualBoardTile = getTileInAPosition(boardTile.position.coord._1+1+direction.shift._1, boardTile.position.coord._2+1+direction.shift._2)
    while(!actualBoardTile.card.equals(boardConstants.defaultCard) && actualBoardTile.position.isValidPosition()){
      if(direction.equals(Directions.N) || direction.equals(Directions.W)) {
        wordReturn = (actualBoardTile.card, actualBoardTile.position.bonus) +: wordReturn
      } else if (direction.equals(Directions.S) || direction.equals(Directions.E)) {
        wordReturn = wordReturn :+ (actualBoardTile.card, actualBoardTile.position.bonus)
      }
      actualBoardTile = getTileInAPosition(actualBoardTile.position.coord._1+1+direction.shift._1, actualBoardTile.position.coord._2+1+direction.shift._2)
    }
    wordReturn
  }

  // metodo di utilità per ottenere da una BoardTile una tupla (Card, String)
  private def boardTails2Tuple(boardTile: BoardTile): (Card, String) = (boardTile.card, boardTile.position.bonus)

  // metodo per ottenere la parola in formato stringa dalla lista di parole trovate con takeCardToCalculatePoints
  def getWordsFromLetters(words: List[ArrayBuffer[(Card, String)]]): List[String] = for( word <- words) yield getWordFromLetters(word)
  private def getWordFromLetters(word: ArrayBuffer[(Card, String)]): String =
    (for (tuple <- word; playedWord <- tuple._1.letter) yield playedWord).mkString("").toLowerCase

  // metodo per il calcolo del punteggio di tutte le parole inserite
  override def calculateTurnPoints(words: List[ArrayBuffer[(Card, String)]]): Int = {
    var result:Int = 0
    words.foreach(result += calculateWordScore(_))
    result
  }

  private def calculateWordScore(word: ArrayBuffer[(Card, String)]): Int =  {
    var letterValue: Int = 0
    var multiplier: Int = 0
    val letterPoints = for (tuple <- word;
                            multiplierBonus = getWordMultiplier(tuple._2);
                            letterValue = getLetterMultiplier(tuple._2) * tuple._1.score) yield (letterValue, multiplierBonus)
    letterPoints.foreach({letterValue += _._1})
    letterPoints.foreach({multiplier += _._2})
    if(multiplier == 0) multiplier=1
    letterValue * multiplier * firstWord+ lenghtBonus(word) + wordIsScarabeoBonus(word)
  }

  // metodo per il bonus che ritorna il moltiplicatore del punteggio della parola
  private def getWordMultiplier(positionBonus: String): Int = positionBonus match{
    case constants.wordForTwo => 2
    case constants.wordForThree => 3
    case _ => 0
  }

  // metodo per il moltiplicatore del punteggio della lettera
  private def getLetterMultiplier(positionBonus: String): Int = positionBonus match{
    case constants.letterForTwo => 2
    case constants.letterForThree => 3
    case _ => 1
  }

  // medoto per il bonus dato dato dalla lunghezza della parola
  private def lenghtBonus(word: ArrayBuffer[(Card, String)]) : Int = word.length match{
    case 8 => constants.bonusLenght8 + isThereScarabeoCard(word)
    case 7 => constants.bonusLenght7 + isThereScarabeoCard(word)
    case 6 => constants.bonusLenght6 + isThereScarabeoCard(word)
    case _ => 0
  }

  // metodo per il bonus se nella parola non è stato usato lo scarabeo
  private def isThereScarabeoCard(word: ArrayBuffer[(Card, String)]): Int =
    if (word exists (tuple => tuple._1.letter == constants.scarabeo)) 0 else constants.bonusWithoutScarabeo
  // metodo per il bonus parol
  //a == SCARABEO
  private def wordIsScarabeoBonus(word: ArrayBuffer[(Card, String)]): Int =  if (getWordFromLetters(word).equals(constants.scarabeoWord)) constants.bonusScarabeoWord else 0
  // bonus prima parola inserita
  private def firstWord(): Int = if(_firstWord){_firstWord = false; constants.firstWordBonus} else 1

  override def playedWordIsOnScarabeo(): Boolean = _playedWord exists(boardTile => boardTile.position.coord.equals(9,9))

  override def lettersAreAdjacent(): Boolean =
    (_playedWord.forall(boardTiles => boardTiles.position.coord._1 == _playedWord.head.position.coord._1 + _playedWord.indexWhere(element => element.equals(boardTiles)))
      != _playedWord.forall(boardTiles => boardTiles.position.coord._2 == _playedWord.head.position.coord._2 + _playedWord.indexWhere(element => element.equals(boardTiles))))
  
}
