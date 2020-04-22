package model

// Direzione in cui ci può movere nel tabellone
sealed trait Direction {
  val shift : (Int, Int)
}

package object Directions {
  case object N extends Direction { val shift: (Int, Int) = (0, -1) }  // Nord
  case object E extends Direction { val shift: (Int, Int) = (1, 0) }   // Est
  case object S extends Direction { val shift: (Int, Int) = (0, 1) }   // Sud
  case object W extends Direction { val shift: (Int, Int) = (-1, 0) }  // Ovest
}

// Posizione di una lettera nella board
sealed trait Position {
  def coord: (Int, Int)
  def bonus: String
  // metodo per muoversi da una posizione specificando delle coordinate numeriche
  def shiftByCoordinates(diff: (Int, Int)) : Option[PositionImpl]
  // metodo per muoversi da una posizione utilizzando una direzione
  def shiftByDirection(dir: Direction) : Option[PositionImpl]
  // metodo per controllare se le coordinate di una posizione sono valide
  def isValidPosition: Boolean
}

// Posizione sulla board
case class PositionImpl(row : Int, col : Int) extends Position {
  val _coord: (Int, Int) = (row - 1, col - 1)
  val _bonus: String = boardConstants.boardBonus.get(row,col).getOrElse("DEFAULT")
  override def coord : (Int, Int) = _coord
  override def bonus : String = _bonus
  // metodo per muoversi da una posizione specificando delle coordinate numeriche
  override def shiftByCoordinates(diff: (Int, Int)) : Option[PositionImpl] =
    Option(PositionImpl(_coord._1 + diff._1, _coord._2 + diff._2))
  // metodo per muoversi da una posizione utilizzando una direzione
  override def shiftByDirection(dir: Direction) : Option[PositionImpl] =
    shiftByCoordinates(dir.shift)
  // metodo per controllare se le coordinate di una posizione sono valide
  override def isValidPosition : Boolean = _coord match {
    case (_,_) if _coord._1 >= 0 && _coord._1 <=16 && _coord._2 >= 0 && _coord._2 <=16 => true
    case _ => false
  }
}
