package model

import scala.collection.mutable.ArrayBuffer
import model.Directions.{N,S,W,E}

package object boardConstants{
  val boardBonus: Map[(Int, Int), String] = Map((1, 5) -> scoreConstants.letterForTwo, (1, 13) -> scoreConstants.letterForTwo, (3, 8) -> scoreConstants.letterForTwo,
    (3, 10) -> scoreConstants.letterForTwo, (4, 9) -> scoreConstants.letterForTwo, (5, 1) ->scoreConstants.letterForTwo,
    (5, 17) ->scoreConstants.letterForTwo, (8, 3) -> scoreConstants.letterForTwo,(8, 8) ->scoreConstants.letterForTwo,
    (8, 10) ->scoreConstants.letterForTwo, (8, 15) ->scoreConstants.letterForTwo, (9, 4) ->scoreConstants.letterForTwo,
    (9, 14) ->scoreConstants.letterForTwo, (10, 3) ->scoreConstants.letterForTwo, (10, 8) ->scoreConstants.letterForTwo,
    (10, 10) ->scoreConstants.letterForTwo, (10, 15) ->scoreConstants.letterForTwo, (13, 1) ->scoreConstants.letterForTwo,
    (13, 17) ->scoreConstants.letterForTwo, (14, 9) ->scoreConstants.letterForTwo, (15, 8) ->scoreConstants.letterForTwo,
    (15, 10) ->scoreConstants.letterForTwo, (17, 5) ->scoreConstants.letterForTwo,(17,13) ->scoreConstants.letterForTwo,
    (2, 7) ->scoreConstants.letterForThree, (2, 11) ->scoreConstants.letterForThree, (7, 2) ->scoreConstants.letterForThree,
    (7, 7) ->scoreConstants.letterForThree, (7, 11) ->scoreConstants.letterForThree, (7, 16) ->scoreConstants.letterForThree,
    (11, 2) ->scoreConstants.letterForThree, (11, 7) ->scoreConstants.letterForThree, (11, 11) ->scoreConstants.letterForThree,
    (11, 16) ->scoreConstants.letterForThree, (16, 7) ->scoreConstants.letterForThree, (16, 11) ->scoreConstants.letterForThree,
    (1, 1) ->scoreConstants.wordForThree, (9, 1) ->scoreConstants.wordForThree, (17, 1) ->scoreConstants.wordForThree,
    (1, 9) ->scoreConstants.wordForThree, (17, 9) ->scoreConstants.wordForThree, (1, 17) ->scoreConstants.wordForThree,
    (9, 17) ->scoreConstants.wordForThree, (17, 17) ->scoreConstants.wordForThree,
    (2, 2) ->scoreConstants.wordForTwo, (3, 3) ->scoreConstants.wordForTwo, (4, 4) ->scoreConstants.wordForTwo,
    (5, 5) ->scoreConstants.wordForTwo, (6, 6) ->scoreConstants.wordForTwo, (12, 12) ->scoreConstants.wordForTwo,
    (13, 13) ->scoreConstants.wordForTwo, (14, 14) ->scoreConstants.wordForTwo, (15, 15) ->scoreConstants.wordForTwo,
    (16, 16) ->scoreConstants.wordForTwo, (2, 16) ->scoreConstants.wordForTwo, (3, 15) ->scoreConstants.wordForTwo,
    (4, 14) ->scoreConstants.wordForTwo, (5, 13) ->scoreConstants.wordForTwo, (6, 12) ->scoreConstants.wordForTwo,
    (12, 6) ->scoreConstants.wordForTwo, (13, 5) ->scoreConstants.wordForTwo, (14, 4) ->scoreConstants.wordForTwo,
    (15, 3) ->scoreConstants.wordForTwo, (16, 2) ->scoreConstants.wordForTwo)


  val boardTileDefault = BoardTileImpl(PositionImpl(-1, -1), cardConstants.defaultCard)

  val horizontal  = "H"
  val vertical = "V"
  val diagonal = "D"
}

// BoardTile: casella all'interno del tabellone
// fa riferimento ad una posizione e poi, quando viene aggiunta, ad una Card
sealed trait BoardTile{
  def card: Card
  def position: PositionImpl
}

case class BoardTileImpl(_position: PositionImpl, _card: Card) extends BoardTile{
  override def position: PositionImpl  = _position
  override def card: Card = _card
}

// Board: implementazione del modello del tabellone
sealed trait Board{
  def boardTiles: List[BoardTile]
  def playedWord: List[BoardTile]
  // metodi per inserire e rimuovere elementi dalla board
  def addCard2Tile (card: Card, x:Int, y:Int, add2PlayedWord:Boolean = false)
  def addPlayedWord(playedWordsList: List[BoardTile])
  def clearPlayedWords()
  def clearBoardFromPlayedWords()
  // metodo per il controllo della prima parola inserita nella partita
  def checkGameFirstWord(): Boolean
  // metodo per estrarre dalla board le parole da controllare
  def takeCardToCalculatePoints(isfirstWord: Boolean = false):  List[ArrayBuffer[(Card, String)]]
  // metodo per convertire le lettere giocate in stringhe corrispondenti alle parole che formano
  def getWordsFromLetters(word: List[ArrayBuffer[(Card, String)]]): List[String]
  // metodo per calcolare il punteggio di un turno
  def calculateTurnPoints(words: List[ArrayBuffer[(Card, String)]], isfirstWord: Boolean = false): Int
}

case class BoardImpl() extends Board {
  private var _boardTiles: List[BoardTile] = populateBoard()
  private var _playedWord: List[BoardTile] = List()

  override def boardTiles: List[BoardTile] = _boardTiles
  override def playedWord: List[BoardTile] = _playedWord

  // metodo per il popolamento della board
  private def populateBoard() = (for( x <- 1 to 17; y <- 1 to 17) yield tuple2BoardTile(x, y)).toList
  private def tuple2BoardTile(tuple: (Int, Int)): BoardTile = BoardTileImpl(PositionImpl.apply(tuple._1, tuple._2), cardConstants.defaultCard)

  // METODI PER INSERIRE ELEMENTI NELLA BOARD O NELLE PAROLE GIOCATE
  // metodo per aggiungere una card in una posizione del tabellone
  override def addCard2Tile(card: Card, x:Int, y:Int, add2PlayedWord:Boolean = false): Unit =  {
    _boardTiles = _boardTiles.map {
      element => if (element.equals(getTileInAPosition(x, y))){
        if(add2PlayedWord) _playedWord = BoardTileImpl(PositionImpl(x, y), card) :: _playedWord
        BoardTileImpl(PositionImpl(x, y), card)}
      else element
    }
  }
  // metodi per aggiungere e rimuovere una lista di carte
  override def addPlayedWord(playedWordsList: List[BoardTile]): Unit = {
    _playedWord = List()
    _playedWord = _playedWord ++ playedWordsList
    for(playedWord <- playedWordsList)  addCard2Tile(playedWord.card, playedWord.position._coord._1+1, playedWord.position._coord._2+1)
  }
  override def clearPlayedWords(): Unit = _playedWord = List()
  override def clearBoardFromPlayedWords(): Unit = for(playedWord <- _playedWord) removeCardFromTile(playedWord.position._coord._1+1, playedWord.position._coord._2+1)
  // metodo per rimuovere una card in una posizione del tabellone
  private def removeCardFromTile(x: Int, y: Int, removeFromPlayedWord:Boolean = false): Card = {
    var card: Card = cardConstants.defaultCard
    _boardTiles = _boardTiles.map {
      element => if (element.equals(getTileInAPosition(x, y))) {
        card = element.card
        if(removeFromPlayedWord) _playedWord = _playedWord.filter(boardTile => !boardTile.equals(element))
        BoardTileImpl(PositionImpl(x, y), cardConstants.defaultCard)
      }else element
    }
    card
  }
  private def samePosition(position: PositionImpl, x: Int, y: Int): Boolean = position._coord._1+1 == x && position._coord._2+1 == y
  private def getTileInAPosition(x:Int, y:Int): BoardTile = _boardTiles.find( boardTile => samePosition(boardTile.position,x, y)).getOrElse(boardConstants.boardTileDefault)

  // METODO PER IL CONTROLLO DELLA PRIMA PAROLA INSERITA NELLA PARTITA
  override def checkGameFirstWord(): Boolean = playedWordIsOnScarabeo() && lettersAreAdjacent()
  // 1 => la prima parola deve essere sopra la figura dello scarabeo al centro del tabellone
  private def playedWordIsOnScarabeo(): Boolean = _playedWord exists(boardTile => boardTile.position._coord.equals(8,8))
  // 2 -> le lettere giocate devono essere tutte adiacenti
  private def lettersAreAdjacent(): Boolean = {
    val playedWordOrderedByX = _playedWord.sortWith(_.position._coord._1<_.position._coord._1)
    val playedWordOrderedByY = _playedWord.sortWith(_.position._coord._2<_.position._coord._2)
    (playedWordOrderedByX.forall(boardTiles => boardTiles.position._coord._1 == playedWordOrderedByX.head.position._coord._1 + playedWordOrderedByX.indexWhere(element => element.equals(boardTiles)))
      != playedWordOrderedByY.forall(boardTiles => boardTiles.position._coord._2 == playedWordOrderedByY.head.position._coord._2 + playedWordOrderedByY.indexWhere(element => element.equals(boardTiles))))
  }

  // METODO PER ESTRARRE DALLA BOARD LE PAROLE DA CONTROLLARE
  override def takeCardToCalculatePoints(isFirstWord: Boolean = false): List[ArrayBuffer[(Card, String)]] = {
    // caso 1 e caso 2 controllo delle parole
    if (!(checkBoardLettersNearness() || isFirstWord) || wordDirectionIsDiagonal()) return List()
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
    // caso 3 per il controllo delle parole
    if (playedLettersAreInFoundWords(listOfWords) ) listOfWords.filter(array => array.length > 1) else List()
  }
  // metodo per ottenere da una data posizione le carte inserite in una direzione
  private def tileBoardsInADirection(direction: Direction, boardTile: BoardTile): ArrayBuffer[(Card, String)] = {
    var wordReturn: ArrayBuffer[(Card,String)] = ArrayBuffer()
    var actualBoardTile = getTileInAPosition(boardTile.position._coord._1+1+direction.shift._1, boardTile.position._coord._2+1+direction.shift._2)
    while(!actualBoardTile.card.equals(cardConstants.defaultCard) && actualBoardTile.position.isValidPosition){
      if(direction.equals(Directions.N) || direction.equals(Directions.W)) {
        wordReturn = (actualBoardTile.card, actualBoardTile.position._bonus) +: wordReturn
      } else if (direction.equals(Directions.S) || direction.equals(Directions.E)) {
        wordReturn = wordReturn :+ (actualBoardTile.card, actualBoardTile.position._bonus)
      }
      actualBoardTile = getTileInAPosition(actualBoardTile.position._coord._1+1+direction.shift._1, actualBoardTile.position._coord._2+1+direction.shift._2)
    }
    wordReturn
  }

  // METODI PER IL CONTROLLO DELLE SUCCESSIVE LETTERE INSERITE DURANTE UN TURNO
  // 1 => gli spazi fra le lettere giocate devono essere occupate dalle lettere già inserite nella board
  private def checkBoardLettersNearness(): Boolean =
    _playedWord.exists(boardTile =>
      !getTileInAPosition(x = boardTile.position.shiftByDirection(N).get._coord._1, y = boardTile.position.shiftByDirection(N).get._coord._2).equals(boardConstants.boardTileDefault) ||
        !getTileInAPosition(x = boardTile.position.shiftByDirection(W).get._coord._1, y = boardTile.position.shiftByDirection(W).get._coord._2).equals(boardConstants.boardTileDefault) ||
        !getTileInAPosition(x = boardTile.position.shiftByDirection(E).get._coord._1, y = boardTile.position.shiftByDirection(E).get._coord._2).equals(boardConstants.boardTileDefault) ||
        !getTileInAPosition(x = boardTile.position.shiftByDirection(S).get._coord._1, y = boardTile.position.shiftByDirection(S).get._coord._2).equals(boardConstants.boardTileDefault)
    )
  // 2 => le lettere inserite devo essere su una colonna o su una riga
  private def wordDirectionIsDiagonal(): Boolean = wordDirection(_playedWord) == boardConstants.diagonal
  // metodo per controllare che le lettere inserite siano tutte nella stessa direzione
  private def wordDirection(wordList: List[BoardTile]): String =
    if (wordList.forall(boardTiles => boardTiles.position._coord._1 == wordList.head.position._coord._1))
      boardConstants.vertical
    else if (wordList.forall(boardTiles => boardTiles.position._coord._2 == wordList.head.position._coord._2))
      boardConstants.horizontal
    else boardConstants.diagonal
  // 3 => una parola, fra quelle trovate, deve contenere tutte le lettere giocate
  private def playedLettersAreInFoundWords(foundWords: List[ArrayBuffer[(Card, String)]]): Boolean =
    foundWords.exists(word => {_playedWord.forall(tileBoard => word.contains((tileBoard.card,tileBoard.position._bonus)))})

  // metodo di utilità per ottenere da una BoardTile una tupla (Card, String)
  private def boardTails2Tuple(boardTile: BoardTile): (Card, String) = (boardTile.card, boardTile.position._bonus)

  // METODO PER CONVERTIRE LE LETTERE GICOATE IN STRINGHE CORRISPONDETI ALLE PAROLE CHE FORMANO
  def getWordsFromLetters(words: List[ArrayBuffer[(Card, String)]]): List[String] = for( word <- words) yield getWordFromLetters(word)
  private def getWordFromLetters(word: ArrayBuffer[(Card, String)]): String =
    (for (tuple <- word; playedWord <- tuple._1.letter) yield playedWord).mkString("").toLowerCase

  // METODO PER CALCOLARE IL PUNTEGGIO DI UN TURNO
  override def calculateTurnPoints(words: List[ArrayBuffer[(Card, String)]], isFirstWord: Boolean = false): Int = (for (word <- words) yield calculateWordScore(word, isFirstWord)).sum
  // metodo per il calcolo del punteggio di una parola
  private def calculateWordScore(word: ArrayBuffer[(Card, String)], isFirstWord: Boolean): Int =  {
    var letterValue: Int = 0
    var multiplier: Int = 0
    val letterPoints = for (tuple <- word;
                            multiplierBonus = scoreRules.wordMultiplier(tuple._2);
                            letterValue = scoreRules.letterMultiplier(tuple._2) * tuple._1.score) yield (letterValue, multiplierBonus)
    letterPoints.foreach(letterValue += _._1)
    letterPoints.foreach(multiplier += _._2)
    if(multiplier == 0) multiplier=1
    letterValue * multiplier * firstWord(isFirstWord)+ scoreRules.lenghtBonus(word) + scoreRules.wordScarabeoBonus(word)
  }
  // metodo per il bonus della prima parola inserita
  private def firstWord(isFirstWord: Boolean): Int = if(isFirstWord) scoreRules.bonusFirstWord() else 1
}
