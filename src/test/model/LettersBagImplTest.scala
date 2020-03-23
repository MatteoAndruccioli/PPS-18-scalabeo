package model

import org.scalatest.FlatSpec
import scala.collection.mutable.ArrayBuffer

class LettersBagImplTest extends FlatSpec {
  "tuple2Cards from lettersScoresCardinalities " should " create a list of letter from constant" in {
    val lettersBagImpl = LettersBagImpl()
    val cardList = lettersBagImpl.tuple2Cards(constants.lettersScoresCardinalities.head)
    assert(cardList.length == constants.lettersScoresCardinalities.head._3)
    assert(cardList.head.letter == constants.lettersScoresCardinalities.head._1)
    assert(cardList.head.score == constants.lettersScoresCardinalities.head._2)
  }

  "the bag of card " should " not be empty" in {
    val lettersBagImpl = LettersBagImpl()
    assert(!lettersBagImpl.bag.isEmpty)
  }

  "The bag in the LettersBag " should "be equivalent to the constant specified" in {
    val lettersScoresExample = List(("A",1,2), ("B",4,1), ("C",1,1), ("D",1,1), ("C",1,1))
    val bag = List(CardImpl("A"), CardImpl("A"),CardImpl("B"), CardImpl("C"), CardImpl("D"), CardImpl("C"))
    val lettersBagImpl = LettersBagImpl()
    val generatedBag = lettersBagImpl.populateBag(lettersScoresExample)
    assert(generatedBag.equals(bag))
  }

  "Take a Card from an empty bag " should "return Empty" in {
    val lettersBagImpl = LettersBagImpl()
    lettersBagImpl.takeRandomElementFromBagOfLetters(130)
    val lastCard = lettersBagImpl.takeRandomElementFromBagOfLetters(1)
    assert(lastCard.equals(Option.empty))
  }

  "N cards " should "be removed from the bag" in {
    val defaultBagLenght = 130
    val numberCardToBeRemoved= 5
    val lettersBagImpl = LettersBagImpl()
    lettersBagImpl.takeRandomElementFromBagOfLetters(numberCardToBeRemoved)
    assert(lettersBagImpl.bag.length == defaultBagLenght-numberCardToBeRemoved)
  }

  "A card " should "be insert in the bag" in {
    val cardA: Card= CardImpl("$")
    val lettersBagImpl = LettersBagImpl()
    lettersBagImpl.reinsertCardInBag(ArrayBuffer(cardA))
    assert(lettersBagImpl.bag.contains(cardA))
  }

}