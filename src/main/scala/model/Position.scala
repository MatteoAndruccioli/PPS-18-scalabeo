package model


// possibile direzione con cui ci si può muovere nel tabellone
sealed trait Direction {
  val shift : (Int, Int)
}

package object Directions {
  case object N  extends Direction { val shift = (0, -1) }  // Nord
  case object E  extends Direction { val shift = (1, 0) }  // Est
  case object S  extends Direction { val shift = (0, 1) } // Sud
  case object W  extends Direction { val shift = (-1, 0) } // Ovest

  val all: Set[Direction] = Set[Direction](N, E,  S,  W)
}


// Posizione sulla board
case class Position(row : Int, col : Int) {
  val coord: (Int, Int) = (row - 1, col - 1)
  val bonus: String = boardConstants.boardBonus.get(row,col).getOrElse("DEFAULT")
  def diff(pos : Position): (Int, Int) = {
    val c1 = coord
    val c2 = pos.coord
    (c1._1 - c2._1, c1._2 - c2._2)
  }
  def getBonus : String = bonus
  def shift(diff: (Int, Int)) : Option[Position] =
    Option(Position(coord._1 + diff._1, coord._2 + diff._2))
  def shift(dir: Direction) : Option[Position] =
    shift(dir.shift)
  def isValidPosition() : Boolean = (coord._1, coord._2) match {
    case (_,_) if coord._1 >= 0 && coord._1 <=16 && coord._2 >= 0 && coord._2 <=16 => true
    case _ => false
  }


}

object Position {
  def direction(dst: Position, src: Position) : Option[Direction] = {
    val diff = dst.diff(src)
    diff match {
      // mosse verticali
      case (0, y) if y != 0 =>
        if (y > 0) Option(Directions.N) else Option(Directions.S)
      // mosse orizzontani
      case (x, 0) if x != 0 =>
        if (x > 0) Option(Directions.E) else Option(Directions.W)
      case _ => None
    }
  }
}


class PositionIterator(pos: Position, dir: Direction, until: Option[Position]) extends Iterator[Position] {
  var current: Position = pos
  def hasNext: Boolean = {
    val next = current.shift(dir)
    !next.isEmpty && (until.isEmpty || next != until)
  }
  def next(): Position = {
    current = current.shift(dir).get
    current
  }
}