package model

import org.scalatest._


class CardTest extends FlatSpec {
  "Letter in a Card " should " be equal to the parameter of the costructor" in {
    val letter = "A"
    val cardA = CardImpl(letter)
    assert(cardA.letter == letter)
  }
  "Score in a Card " should " be assigned according to the game rules" in {
    val letter = "A"
    val score = cardConstants.lettersScoresCardinalities.find(s => s._1 == letter).head._2
    val cardA = CardImpl(letter)
    assert(cardA.score == score)
  }
}
