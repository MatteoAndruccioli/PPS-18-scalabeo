package model

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

  // TODO: metodi per il controllo delle parole inserite
  // TODO: metodi per il calcolo del punteggio delle parole inserite
}

case class BoardImpl() extends Board {
  private var _boardTiles: List[BoardTile] = populateBoard()
  private var _playedWord: List[BoardTile] = List()

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



}