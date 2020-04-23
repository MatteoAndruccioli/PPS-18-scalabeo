package model

import org.scalatest._

class LettersBagTest extends FlatSpec {
  "The letters bag " should " not be empty" in {
    val lettersBagImpl = LettersBagImpl()
    assert(lettersBagImpl.bag.nonEmpty)
  }
  "The letters bag " should "be created according to game rules" in {
    // si testa il meccanismo con la bag usata per i test
    val bag = cardConstants.lettersScoresCardinalities.flatMap(x => List.fill(x._3)(CardImpl(x._1)))
    val lettersBagImpl = LettersBagImpl()
    assert(lettersBagImpl.bag.equals(bag))
  }
  "An empty bag " should " not give a new letter" in {
    val lettersBagImpl = LettersBagImpl()
    lettersBagImpl.takeRandomElementFromBagOfLetters(130)
    val lastCard = lettersBagImpl.takeRandomElementFromBagOfLetters(1)
    assert(lastCard.equals(Option.empty))
  }
  "N letters " should "be removed from the bag" in {
    val defaultBagLenght = 130
    val numberCardToBeRemoved= 5
    val lettersBagImpl = LettersBagImpl()
    lettersBagImpl.takeRandomElementFromBagOfLetters(numberCardToBeRemoved)
    assert(lettersBagImpl.bag.length == defaultBagLenght-numberCardToBeRemoved)
  }
  "A letter " should "be inserted in the bag" in {
    val cardA: Card= CardImpl("$")
    val lettersBagImpl = LettersBagImpl()
    lettersBagImpl.reinsertCardInBag(Vector(cardA))
    assert(lettersBagImpl.bag.contains(cardA))
  }
}