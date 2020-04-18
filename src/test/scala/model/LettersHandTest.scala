package model

import org.scalatest._

import scala.collection.mutable.ArrayBuffer

class LettersHandTest extends FlatSpec {
  "A new letter " should " be added in a specific position of the hand" in {
    val hand:ArrayBuffer[Card] = ArrayBuffer(CardImpl("A"))
    val lettersHand = LettersHandImpl(hand)
    lettersHand.putLetter(cardPosition = 0,CardImpl("B"))
    assert(lettersHand.hand.head == CardImpl("B"))
  }
  "A letter " should " be played from the hand" in {
    val hand:ArrayBuffer[Card] = ArrayBuffer(CardImpl("A"), CardImpl("B"))
    val lettersHand = LettersHandImpl(hand)
    val card = lettersHand.playLetter(cardPosition = 1)
    assert(card.equals(CardImpl("B")) && !lettersHand.hand.contains(CardImpl("B")))
  }
  "The hand " should " be changed" in {
    val hand:ArrayBuffer[Card] = ArrayBuffer(CardImpl("A"), CardImpl("B"))
    val hand1:ArrayBuffer[Card] = ArrayBuffer(CardImpl("A"), CardImpl("B"))
    val lettersHand = LettersHandImpl(hand)
    lettersHand.changeHand(hand1)
    assert(lettersHand.hand == hand1)
  }
  "The hand points" should " be calculated" in {
    val hand:ArrayBuffer[Card] = ArrayBuffer(CardImpl("A"), CardImpl("B"))
    val lettersHand = LettersHandImpl(hand)
    val points = 5
    assert(lettersHand.calculateHandPoint == points)
  }
  "Hand " should "be checked if it contains only vowls or only consonants" in {
    val vocalHand:ArrayBuffer[Card] = ArrayBuffer(CardImpl("A"), CardImpl("E"))
    val constantHand:ArrayBuffer[Card] = ArrayBuffer(CardImpl("C"), CardImpl("D"))
    val lettersHand = LettersHandImpl(vocalHand)
    assert(lettersHand.containsOnlyVowelsOrOnlyConsonants())
    lettersHand.changeHand(constantHand)
    assert(lettersHand.containsOnlyVowelsOrOnlyConsonants())
    lettersHand.putLetter(cardPosition = 1,CardImpl("A"))
    assert(!lettersHand.containsOnlyVowelsOrOnlyConsonants())
  }
}
