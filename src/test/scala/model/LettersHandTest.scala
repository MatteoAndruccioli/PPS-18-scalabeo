package model

import org.scalatest._


class LettersHandTest extends FlatSpec {
  "A new letter " should " be added in a specific position of the hand" in {
    val hand:Vector[Card] = Vector(CardImpl("A"))
    val lettersHand = LettersHandImpl(hand)
    lettersHand.putLetter(cardPosition = 0,CardImpl("B"))
    assert(lettersHand.hand.head == CardImpl("B"))
  }
  "A letter " should " be played from the hand" in {
    val hand:Vector[Card] = Vector(CardImpl("A"), CardImpl("B"))
    val lettersHand = LettersHandImpl(hand)
    val card = lettersHand.playLetter(cardPosition = 1)
    assert(card.equals(CardImpl("B")) && !lettersHand.hand.contains(CardImpl("B")))
  }
  "A new letter " should " be added in a specific position of the hand after one is already played" in {
    val hand:Vector[Card] = Vector(CardImpl("A"), CardImpl("B"), CardImpl("C"))
    val lettersHand = LettersHandImpl(hand)
    lettersHand.playLetter(cardPosition = 1)
    lettersHand.putLetter(cardPosition = 1,CardImpl("B"))
    assert(lettersHand.hand == hand)
  }
  "A new letter " should " be added in a specific position of the hand after one is already played: case 2" in {
    val hand:Vector[Card] = Vector(CardImpl("A"), CardImpl("B"), CardImpl("C"))
    val lettersHand = LettersHandImpl(hand)
    lettersHand.playLetter(cardPosition = 2)
    lettersHand.putLetter(cardPosition = 2,CardImpl("C"))
    assert(lettersHand.hand == hand)
  }
  "The hand " should " be changed" in {
    val hand:Vector[Card] = Vector(CardImpl("A"), CardImpl("B"))
    val hand1:Vector[Card] = Vector(CardImpl("A"), CardImpl("B"))
    val lettersHand = LettersHandImpl(hand)
    lettersHand.changeHand(hand1)
    assert(lettersHand.hand == hand1)
  }
  "The hand points" should " be calculated" in {
    val hand:Vector[Card] = Vector(CardImpl("A"), CardImpl("B"))
    val lettersHand = LettersHandImpl(hand)
    val points = 5
    assert(lettersHand.calculateHandPoint == points)
  }
  "Hand " should "be checked if it contains only vowls or only consonants" in {
    val vocalHand:Vector[Card] = Vector(CardImpl("A"), CardImpl("E"))
    val constantHand:Vector[Card] = Vector(CardImpl("C"), CardImpl("D"))
    val lettersHand = LettersHandImpl(vocalHand)
    assert(lettersHand.containsOnlyVowelsOrOnlyConsonants())
    lettersHand.changeHand(constantHand)
    assert(lettersHand.containsOnlyVowelsOrOnlyConsonants())
    lettersHand.putLetter(cardPosition = 1,CardImpl("A"))
    assert(!lettersHand.containsOnlyVowelsOrOnlyConsonants())
  }
}
