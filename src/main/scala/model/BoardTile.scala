package model

import model.Directions.{N,S,W,E}

/** Costanti per il tabellone
  * - boardBonus: mappa che associa alle posizioni un bonus secondo le regole dello scarabeo
  * - boardTileDefault: valore di default della casella del tabellone
  * - horizontal, vertical, diagonal: direzioni delle lettere inserite nel tabellone
  */
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

/** Casella all'interno del tabellone
  * - card: lettera in quella casella
  * - position: posizione nel tabellone
  */
sealed trait BoardTile{
  def card: Card
  def position: Position
}

/** Implementazione della casella all'interno del tabellone
  * @param _position : posizione nel tabellone
  * @param _card : lettera da inserire nella casella
  */
case class BoardTileImpl(_position: Position, _card: Card) extends BoardTile{
  override def position: Position  = _position
  override def card: Card = _card
}

/** Tabellone di gioco
  * - boardTiles: caselle nel tabellone
  * - playedWord: parole giocate in un turno nel tabellone (parola giocata)
  * - addCard2Tile, addPlayedWord, clearPlayedWords, clearBoardFromPlayedWords: metodi per inserire e rimuovere elementi dalla board
  * - checkGameFirstWord: metodo per il controllo della prima parola inserita nella partita
  * - takeCardToCalculatePoints: metodo per estrarre dalla board le parole da controllare
  * - getWordsFromLetters: metodo per convertire le lettere giocate in stringhe corrispondenti alle parole che formano
  * - calculateTurnPoints: metodo per calcolare il punteggio di un turno
  */
sealed trait Board{
  def boardTiles: List[BoardTile]
  def playedWord: List[BoardTile]
  def addCard2Tile (card: Card, x:Int, y:Int, add2PlayedWord:Boolean = false)
  def addPlayedWord(playedWordsList: List[BoardTile])
  def clearPlayedWords()
  def clearBoardFromPlayedWords()
  def checkGameFirstWord(): Boolean
  def takeCardToCalculatePoints(isfirstWord: Boolean = false):  List[List[(Card, String)]]
  def getWordsFromLetters(word: List[List[(Card, String)]]): List[String]
  def calculateTurnPoints(words: List[List[(Card, String)]], isfirstWord: Boolean = false): Int
}

case class BoardImpl() extends Board {
  private var _boardTiles: List[BoardTile] = populateBoard()
  private var _playedWord: List[BoardTile] = List()

  /** metodo per accedere alle caselle della board
    * @return caselle della board
    */
  override def boardTiles: List[BoardTile] = _boardTiles
  /** metodo per accedere alle lettere giocate in un turno
    * @return lettere giocate (parola giocata)
    */
  override def playedWord: List[BoardTile] = _playedWord

  // metodo per il popolamento del tabellone
  private def populateBoard() = (for( x <- 1 to 17; y <- 1 to 17) yield tuple2BoardTile(x, y)).toList
  private def tuple2BoardTile(tuple: (Int, Int)): BoardTile = BoardTileImpl(PositionImpl(tuple._1, tuple._2), cardConstants.defaultCard)

  /** metodo per aggiungere una lettera in una posizione del tabellone
    * @param card lettera da inserire
    * @param x ascissa in cui inserire la lettera
    * @param y ordinata in cui inserire la lettera
    * @param add2PlayedWord aggiungere o meno la lettera alla lista di lettere giocate
    */
  override def addCard2Tile(card: Card, x:Int, y:Int, add2PlayedWord:Boolean = false): Unit =  {
    _boardTiles = _boardTiles.map {
      element => if (element.equals(getTileInAPosition(x, y))){
        if(add2PlayedWord) _playedWord = BoardTileImpl(PositionImpl(x, y), card) :: _playedWord
        BoardTileImpl(PositionImpl(x, y), card)}
      else element
    }
  }
  /** metodo per aggiungere una lista di lettere a quelle giocate
    * @param playedWordsList lista di lettere da inserire
    */
  override def addPlayedWord(playedWordsList: List[BoardTile]): Unit = {
    _playedWord = List()
    _playedWord = _playedWord ++ playedWordsList
    for(playedWord <- playedWordsList)  addCard2Tile(playedWord.card, playedWord.position.coord._1+1, playedWord.position.coord._2+1)
  }
  /** metodo per svuotare la lista delle lettere giocate */
  override def clearPlayedWords(): Unit = _playedWord = List()
  /** metodo per pulire la board dalle lettere giocate */
  override def clearBoardFromPlayedWords(): Unit = for(playedWord <- _playedWord) removeCardFromTile(playedWord.position.coord._1+1, playedWord.position.coord._2+1)
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

  private def samePosition(position: Position, x: Int, y: Int): Boolean = position.coord._1+1 == x && position.coord._2+1 == y
  private def getTileInAPosition(x:Int, y:Int): BoardTile = _boardTiles.find( boardTile => samePosition(boardTile.position,x, y)).getOrElse(boardConstants.boardTileDefault)

  /** metodo per il controllo della prima parola inserita nella partita
    * 1 => la prima parola deve essere sopra la figura dello scarabeo al centro del tabellone
    * 2 => le lettere giocate devono essere tutte adiacenti
    * @return vero se si passano i controlli, falso altrimenti
    */
  override def checkGameFirstWord(): Boolean = playedWordIsOnScarabeo() && lettersAreAdjacent()
  // 1 => la prima parola deve essere sopra la figura dello scarabeo al centro del tabellone
  private def playedWordIsOnScarabeo(): Boolean = _playedWord exists(boardTile => boardTile.position.coord.equals(8,8))
  // 2 => le lettere giocate devono essere tutte adiacenti
  private def lettersAreAdjacent(): Boolean = {
    val playedWordOrderedByX = _playedWord.sortWith(_.position.coord._1 < _.position.coord._1)
    val playedWordOrderedByY = _playedWord.sortWith(_.position.coord._2 < _.position.coord._2)
    (playedWordOrderedByX.forall(boardTiles => boardTiles.position.coord._1 == playedWordOrderedByX.head.position.coord._1 + playedWordOrderedByX.indexWhere(element => element.equals(boardTiles)))
      != playedWordOrderedByY.forall(boardTiles => boardTiles.position.coord._2 == playedWordOrderedByY.head.position.coord._2 + playedWordOrderedByY.indexWhere(element => element.equals(boardTiles))))
  }

  /** metodo per estrarre dal tabellone le parole da controllare partendo dalla lista delle lettere inserite_
    * - per la testa della lista controllo in tutti e due i versi
    * - se le carte giocate sono di più allora devo cercare anche le parole formate dagli incroci:
    *    * le carte giocate sono in orizzontale -> si cercano gli incroci in S/N (verticale)
    *    * le carte messe sono in verticale -> si cercano gli incroci in W/E (orizzontale)
    * Inoltre si controlla che:
    * - gli spazi fra le lettere giocate devono essere occupate dalle lettere già inserite nella board (caso 1)
    * - le lettere inserite devo essere su una colonna o su una riga (caso 2)
    * - una parola, fra quelle trovate, deve contenere tutte le lettere giocate (caso 3)
    * @param isFirstWord flag per affermare se è la prima parola da controllare
    * @return lista delle parole trovate
    */
  override def takeCardToCalculatePoints(isFirstWord: Boolean = false): List[List[(Card, String)]] = {
    // caso 1 e caso 2 controllo delle parole
    if (!(checkBoardLettersNearness() || isFirstWord) || wordDirectionIsDiagonal()) return List()
    var listOfWords: List[List[(Card, String)]] = List()
    // per la testa della lista controllo in tutti e due i versi
    listOfWords = (tileBoardsInADirection(Directions.N, _playedWord.head)++ List(boardTails2Tuple(_playedWord.head)) ++ tileBoardsInADirection(Directions.S, _playedWord.head)) :: listOfWords
    listOfWords = (tileBoardsInADirection(Directions.W, _playedWord.head)++ List(boardTails2Tuple(_playedWord.head)) ++ tileBoardsInADirection(Directions.E, _playedWord.head)) :: listOfWords
    // se le carte giocate sono di più allora devo cercare anche le parole formate dagli incroci
    if (_playedWord.length > 1) {
      if (wordDirection(_playedWord) == boardConstants.horizontal) {
        // le carte messe sono in orizzontale
        // si cercano gli incroci in S/N (verticale)
        for(boardTile <- _playedWord.tail){
          listOfWords = (tileBoardsInADirection(Directions.N, boardTile)++ List(boardTails2Tuple(boardTile)) ++ tileBoardsInADirection(Directions.S, boardTile)) :: listOfWords
        }
      } else if (wordDirection(_playedWord) == boardConstants.vertical) {
        // le carte messe sono in verticale
        // si cercano gli incroci in W/E (orizzontale)
        for(boardTile <- _playedWord.tail) {
          listOfWords = (tileBoardsInADirection(Directions.W, boardTile) ++ List(boardTails2Tuple(boardTile)) ++ tileBoardsInADirection(Directions.E, boardTile)) :: listOfWords
        }
      }
    }
    // caso 3 per il controllo delle parole
    if (playedLettersAreInFoundWords(listOfWords) ) listOfWords.filter(array => array.length > 1) else List()
  }
  // metodo per ottenere da una data posizione le carte inserite in una direzione
  private def tileBoardsInADirection(direction: Direction, boardTile: BoardTile): List[(Card, String)] = {
    var wordReturn: List[(Card,String)] = List()
    var actualBoardTile = getTileInAPosition(boardTile.position.coord._1+1+direction.shift._1, boardTile.position.coord._2+1+direction.shift._2)
    while(!actualBoardTile.card.equals(cardConstants.defaultCard) && actualBoardTile.position.isValidPosition){
      if(direction.equals(Directions.N) || direction.equals(Directions.W)) {
        wordReturn = (actualBoardTile.card, actualBoardTile.position.bonus) +: wordReturn
      } else if (direction.equals(Directions.S) || direction.equals(Directions.E)) {
        wordReturn = wordReturn :+ (actualBoardTile.card, actualBoardTile.position.bonus)
      }
      actualBoardTile = getTileInAPosition(actualBoardTile.position.coord._1+1+direction.shift._1, actualBoardTile.position.coord._2+1+direction.shift._2)
    }
    wordReturn
  }

  // METODI PER IL CONTROLLO DELLE SUCCESSIVE LETTERE INSERITE DURANTE UN TURNO
  // 1 => gli spazi fra le lettere giocate devono essere occupate dalle lettere già inserite nella board
  private def checkBoardLettersNearness(): Boolean =
    _playedWord.exists(boardTile =>
      !getTileInAPosition(x = boardTile.position.shiftByDirection(N).get.coord._1, y = boardTile.position.shiftByDirection(N).get.coord._2).equals(boardConstants.boardTileDefault) ||
        !getTileInAPosition(x = boardTile.position.shiftByDirection(W).get.coord._1, y = boardTile.position.shiftByDirection(W).get.coord._2).equals(boardConstants.boardTileDefault) ||
        !getTileInAPosition(x = boardTile.position.shiftByDirection(E).get.coord._1, y = boardTile.position.shiftByDirection(E).get.coord._2).equals(boardConstants.boardTileDefault) ||
        !getTileInAPosition(x = boardTile.position.shiftByDirection(S).get.coord._1, y = boardTile.position.shiftByDirection(S).get.coord._2).equals(boardConstants.boardTileDefault)
    )
  // 2 => le lettere inserite devo essere su una colonna o su una riga
  private def wordDirectionIsDiagonal(): Boolean = wordDirection(_playedWord) == boardConstants.diagonal
  // metodo per controllare che le lettere inserite siano tutte nella stessa direzione
  private def wordDirection(wordList: List[BoardTile]): String =
    if (wordList.forall(boardTiles => boardTiles.position.coord._1 == wordList.head.position.coord._1))
      boardConstants.vertical
    else if (wordList.forall(boardTiles => boardTiles.position.coord._2 == wordList.head.position.coord._2))
      boardConstants.horizontal
    else boardConstants.diagonal
  // 3 => una parola, fra quelle trovate, deve contenere tutte le lettere giocate
  private def playedLettersAreInFoundWords(foundWords: List[List[(Card, String)]]): Boolean =
    foundWords.exists(word => {_playedWord.forall(tileBoard => word.contains((tileBoard.card,tileBoard.position.bonus)))})

  // metodo di utilità per ottenere da una BoardTile una tupla (Card, String)
  private def boardTails2Tuple(boardTile: BoardTile): (Card, String) = (boardTile.card, boardTile.position.bonus)

  /** metodo per convertire le parole trovate in stringhe corrispondenti alle parole che formano
    * @param words : lista di parole trovare
    * @return le parole in formato di stringhe
    */
  def getWordsFromLetters(words: List[List[(Card, String)]]): List[String] = for( word <- words) yield getWordFromLetters(word)
  private def getWordFromLetters(word: List[(Card, String)]): String =
    (for (tuple <- word; playedWord <- tuple._1.letter) yield playedWord).mkString("").toLowerCase

  /** metodo per il calcolo del punteggio di una parola secondo le regole dello scarabeo:
    * - si sommano i bonus per moltiplicare il valore della parola (multiplierBonus)
    * - si moltiplicano i punteggi delle lettere per i bonus sulla singola lettera (letterValue)
    * - si calcola il punteggio totale sommando anche i bonus per la lunghezza, per la presenza dello scarabeo
    *   e per il bonus se si forma la parola scarabeo
    * - il punteggio totale è dato da
    *    letterValue * multiplier * firstWord + lenghtBonus + wordScarabeoBonus
    * @param words
    * @param isFirstWord
    * @return
    */
  override def calculateTurnPoints(words: List[List[(Card, String)]], isFirstWord: Boolean = false): Int = (for (word <- words) yield calculateWordScore(word, isFirstWord)).sum
  // metodo per il calcolo del punteggio di una parola
  private def calculateWordScore(word: List[(Card, String)], isFirstWord: Boolean): Int =  {
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
