package model

import model.Directions.E
import org.scalatest._

class PositionTest extends FlatSpec {

  "The Position bonus " should " be equal to bonus of the board" in {
    val x = 1
    val y = 1
    val position = Position(x,y)
    assert(position.bonus.equals(boardConstants.boardBonus.get(x,y).getOrElse("DEFAULT")))
  }

  "Coordinates of position " should " be checked" in {
    val positionGood = Position(1,1)
    val positionWrong = Position(20,20)
    assert(positionGood.isValidPosition() && !positionWrong.isValidPosition())
  }

  "Position " should " be shifed with position or with direction" in {
    val position = Position(1,1)
    val shiftedCoordinatePosition = position.shift(1,0)
    val shiftedDirectionPosition = position.shift(E)
    assert(shiftedCoordinatePosition.equals(shiftedDirectionPosition))
  }
}

