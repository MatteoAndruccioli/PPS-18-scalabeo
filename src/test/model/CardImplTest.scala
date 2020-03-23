package model

import model.constants
import model.CardImpl
import org.scalatest._


class CardImplTest extends FlatSpec {
  "Letter in a Card " should " be equal to the parameter of the costructor" in {
    val letter = "A";
    val cardA = CardImpl(letter)
    assert(cardA.letter == letter)
  }

  "Score in a Card " should " be equal to the value of letter in lettersScoresCardinalities" in {
    val letter = "A";
    val score = constants.lettersScoresCardinalities.find(s => s._1 == letter).head._2
    val cardA = CardImpl(letter)
    assert(cardA.score == score)
  }
}
