package model

import org.scalatest.FlatSpec

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

  "populateBag " should "create a list of word from constant" in {
    val lettersScoresExample = List(("A",1,2), ("B",4,1), ("C",1,1), ("D",1,1), ("C",1,1))
    val bag = List(CardImpl("A"), CardImpl("A"),CardImpl("B"), CardImpl("C"), CardImpl("D"), CardImpl("C"))
    val lettersBagImpl = LettersBagImpl()
    val generatedBag = lettersBagImpl.populateBag(lettersScoresExample)
    assert(generatedBag.equals(bag))
  }

}