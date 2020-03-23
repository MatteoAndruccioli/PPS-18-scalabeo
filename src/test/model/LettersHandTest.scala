package model

import org.scalatest._

import scala.collection.mutable.ArrayBuffer

class LettersHandTest extends FlatSpec {

  "The hand " should " be equal to Array passed to costructor" in {
    val hand:ArrayBuffer[Card] = ArrayBuffer(CardImpl("A"))
    val lettersHand = LettersHandImpl(hand)
    assert(lettersHand.hand.equals(hand))
  }

  "A letter " should " be in the position specified in the hand" in {
    val hand:ArrayBuffer[Card] = ArrayBuffer(CardImpl("A"))
    val lettersHand = LettersHandImpl(hand)
    lettersHand.putLetter(0,CardImpl("B"))
    assert(lettersHand.hand.head.equals(CardImpl("B")))
  }

  "A letter " should " be played" in {
    val hand:ArrayBuffer[Card] = ArrayBuffer(CardImpl("A"), CardImpl("B"))
    val lettersHand = LettersHandImpl(hand)
    val card = lettersHand.playLetter(1)
    assert(card.equals(CardImpl("B")))
  }

  "the hand " should " be changed" in {
    val hand:ArrayBuffer[Card] = ArrayBuffer(CardImpl("A"), CardImpl("B"))
    val hand1:ArrayBuffer[Card] = ArrayBuffer(CardImpl("A"), CardImpl("B"))
    val lettersHand = LettersHandImpl(hand)
    lettersHand.changeHand(hand1)
    assert(lettersHand.hand.equals(hand1))
  }

  "The hand point" should " be calculated" in {
    val hand:ArrayBuffer[Card] = ArrayBuffer(CardImpl("A"), CardImpl("B"))
    val lettersHand = LettersHandImpl(hand)
    val points = 5
    assert(lettersHand.calculateHandPoint.equals(points))
  }

  "If the hand contains only vowls or constants, it" should "be checked " in {
    val vocalHand:ArrayBuffer[Card] = ArrayBuffer(CardImpl("A"), CardImpl("E"))
    val constantHand:ArrayBuffer[Card] = ArrayBuffer(CardImpl("C"), CardImpl("D"))
    val lettersHand = LettersHandImpl(vocalHand)
    assert(lettersHand.containsOnlyVowelsOrOnlyConsonants())
    lettersHand.changeHand(constantHand)
    assert(lettersHand.containsOnlyVowelsOrOnlyConsonants())
    lettersHand.putLetter(1,CardImpl("A"))
    assert(!lettersHand.containsOnlyVowelsOrOnlyConsonants())
  }


}
