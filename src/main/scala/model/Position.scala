package model

/** Direzione in cui ci si può muovere nel tabellone
  * - shift: spostamento equivalente alla direzione
  */
sealed trait Direction {
  val shift : (Int, Int)
}

/** Implementazione della direzione */
package object Directions {
  case object N extends Direction { val shift: (Int, Int) = (0, -1) }  // Nord
  case object E extends Direction { val shift: (Int, Int) = (1, 0) }   // Est
  case object S extends Direction { val shift: (Int, Int) = (0, 1) }   // Sud
  case object W extends Direction { val shift: (Int, Int) = (-1, 0) }  // Ovest
}

/** Posizione di una lettera nella board
  * - coord: coordinate nel tabellone
  * - bonus: bonus equivalente a quella posizione
  * - shiftByCoordinates: metodo per muoversi da una posizione specificando delle coordinate numeriche
  * - shiftByDirection: metodo per muoversi da una posizione utilizzando una direzione
  * - isValidPosition: metodo per controllare se le coordinate di una posizione sono valide
  */
sealed trait Position {
  def coord: (Int, Int)
  def bonus: String
  def shiftByCoordinates(diff: (Int, Int)) : Option[PositionImpl]
  def shiftByDirection(dir: Direction) : Option[PositionImpl]
  def isValidPosition: Boolean
}

/** Implementazione della posizione
  * @param row: colonna in cui inserire la posizione
  * @param col: riga in cui inserire la posizione
  */
case class PositionImpl(row : Int, col : Int) extends Position {
  private val _coord: (Int, Int) = (row - 1, col - 1)
  private val _bonus: String = boardConstants.boardBonus.get(row,col).getOrElse("DEFAULT")
  /** metodo per accedere alle coordinate della posizione
    * @return coordinate
    */
  override def coord : (Int, Int) = _coord
  /** metodo per accedere al bonus della posizione
    * @return bonus
    */
  override def bonus : String = _bonus
  /** metodo per muoversi da una posizione specificando delle coordinate numeriche
    * @param diff coordinate corrispondenti allo spostamento che si vuole effettuare
    * @return posizione corrispondente allo spostamento voluto
    */
  override def shiftByCoordinates(diff: (Int, Int)) : Option[PositionImpl] =
    Option(PositionImpl(_coord._1 + diff._1, _coord._2 + diff._2))
  /** metodo per muoversi da una posizione utilizzando una direzione
    * @param dir direzione corrispondente allo spostamento che si vuole effettuare
    * @return posizione corrispondente allo spostamento voluto
    */
  override def shiftByDirection(dir: Direction) : Option[PositionImpl] =
    shiftByCoordinates(dir.shift)
  /** metodo per controllare se le coordinate di una posizione sono valide
    * @return vero se la posizione è valida altrimenti falso
    */
  override def isValidPosition : Boolean = _coord match {
    case (_,_) if _coord._1 >= 0 && _coord._1 <=16 && _coord._2 >= 0 && _coord._2 <=16 => true
    case _ => false
  }
}
