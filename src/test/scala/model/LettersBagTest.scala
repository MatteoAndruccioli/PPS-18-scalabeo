package model

import org.scalatest._
import scala.collection.mutable.ArrayBuffer

class LettersBagTest extends FlatSpec {
  "The cards bag " should " not be empty" in {
    val lettersBagImpl = LettersBagImpl()
    assert(lettersBagImpl.bag.nonEmpty)
  }
  "The cards bag " should "be created according to game rules" in {
    // si testa il meccanismo con la bag usata per i test
    val bag = List(CardImpl("I"), CardImpl("I"),CardImpl("I"), CardImpl("I"), CardImpl("I"), CardImpl("I"), CardImpl("I"), CardImpl("I"),
      CardImpl("S"), CardImpl("S"),CardImpl("S"), CardImpl("S"), CardImpl("S"), CardImpl("S"), CardImpl("S"), CardImpl("S"))
    val lettersBagImpl = LettersBagImpl(test= true)
    assert(lettersBagImpl.bag.equals(bag))
  }
  "An empty bag " should " not give a new Card" in {
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
  "A card " should "be inserted in the bag" in {
    val cardA: Card= CardImpl("$")
    val lettersBagImpl = LettersBagImpl()
    lettersBagImpl.reinsertCardInBag(ArrayBuffer(cardA))
    assert(lettersBagImpl.bag.contains(cardA))
  }
}