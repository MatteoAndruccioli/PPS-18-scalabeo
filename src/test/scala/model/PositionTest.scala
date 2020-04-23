package model

import org.scalatest._
import model.Directions.E

class PositionTest extends FlatSpec {
  // il bonus posizione deve essere uguale a quello definito nella costante boardBonus
  "The Position bonus " should " be equal to bonus defined for the position" in {
    val x = 1
    val y = 1
    val position = PositionImpl(x,y)
    assert(position.bonus == boardConstants.boardBonus.get(x,y).getOrElse("DEFAULT"))
  }
  "A validity check " should " be performed on position coordinates" in {
    val positionGood = PositionImpl(1,1)
    val positionWrong = PositionImpl(20,20)
    assert(positionGood.isValidPosition && !positionWrong.isValidPosition)
  }
  "Position on the boord" should " be changed according to the direction" in {
    val position = PositionImpl(1,1)
    val shiftedPositionByDirection = position.shiftByDirection(E)
    assert(shiftedPositionByDirection.get == PositionImpl(1,0))
  }
  "Position on the boord" should " be changed according to new coordinates" in {
    val position = PositionImpl(1,1)
    val shiftedPositionByNumber = position.shiftByCoordinates((1,1))
    assert(shiftedPositionByNumber.get == PositionImpl(1,1))
  }
}

