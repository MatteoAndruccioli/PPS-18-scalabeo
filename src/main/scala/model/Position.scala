package model


// possibile direzione con cui ci si può muovere nel tabellone
sealed trait Direction {
  val shift : (Int, Int)
}

package object Directions {
  case object N  extends Direction { val shift = (0, -1) }  // North
  case object E  extends Direction { val shift = (1, 0) }  // East
  case object S  extends Direction { val shift = (0, 1) } // South
  case object W  extends Direction { val shift = (-1, 0) } // West

  val all: Set[Direction] = Set[Direction](N, E,  S,  W)
}
