package model

import scala.collection.mutable.ArrayBuffer
import scala.util.Random


package object constants {
  // lista del punteggio e della cardinalità che le lettere devono avere in una partita
  // (lettare. valore, cardinalità)
  val lettersScoresCardinalities = List(("A",1,12), ("B",4,4), ("C",1,7), ("D",4,4),
    ("E",1,12), ("F",4,4), ("G",4,4), ("H",8,2),
    ("I",1,12), ("L",2,6), ("M",2,6), ("N",2,6),
    ("O",1,12), ("P",3,4), ("Q",10,2), ("R",1,7),
    ("S",1,7),  ("T",1,7), ("U",4,4), ("V",4,4),
    ("Z",8,2),  ("[a-zA-Z]",1,2))

  // bonus che può avere una card
  val letterForTwo: String = "2L"
  val letterForThree: String = "3L"
  val wordForTwo: String = "2P"
  val wordForThree: String = "3P"
  val bonusLenght8 = 50
  val bonusLenght7 = 30
  val bonusLenght6 = 10
  val scarabeo = "[a-zA-Z]"
  val firstWordBonus = 2
  val bonusScarabeoWord = 100
  val bonusWithoutScarabeo = 10
}

// interfaccia della Carta: lettera e relativo valore
sealed trait Card{
  def letter: String
  def score: Int
}


// implementazione della Carta
case class CardImpl (var _letter : String) extends Card {
  override def letter: String = _letter
  // lo score è assegnato automaticamente usando la lista definita in lettersScoresCardinalities
  override def score: Int = constants.lettersScoresCardinalities.find(s => s._1 == _letter).head._2
}


// Sacchetto contentenente le lettere da poter usare durante una partita
sealed trait LettersBag {
  def bag: List[Card]
  def populateBag(list: List[(String, Int, Int)]): List[Card]
  def tuple2Cards(tuple: (String, Int, Int)): List[Card]
  def reinsertCardInBag(cardsToInsert: ArrayBuffer[Card]): Unit
  def takeRandomElementFromBagOfLetters(lettersToTake: Int): Option[List[Card]]
}


// implementazione LettersBag
case class LettersBagImpl() extends LettersBag {
  private var _bag: List[Card] = populateBag(constants.lettersScoresCardinalities)
  override def populateBag(list: List[(String, Int, Int)]): List[Card] = list.flatMap(tuple2Cards)
  override def tuple2Cards(tuple: (String, Int, Int)): List[Card] = List.fill(tuple._3)(CardImpl(tuple._1))
  override def bag: List[Card] = _bag
  override def reinsertCardInBag(cardsToInsert: ArrayBuffer[Card]): Unit = _bag = _bag ++ cardsToInsert
  def takeRandomElementFromBagOfLetters(lettersToTake: Int): Option[List[Card]] = _bag match {
    case Nil => Option.empty
    case _ =>
      val shuffledList = Random.shuffle(_bag)
      if (lettersToTake > _bag.length) {
        _bag = Nil
        Some(shuffledList)
      } else {
        _bag = shuffledList.drop(lettersToTake)
        Some(shuffledList.slice(0, lettersToTake))
      }
  }
}

// mano delle card di ogni giocatore
sealed trait LettersHand {
  def hand: ArrayBuffer[Card]
  def playLetter (cardPosition: Int): Card
  def putLetter (cardPosition: Int, card: Card)
  def changeHand(newHand: ArrayBuffer[Card])
  def calculateHandPoint: Int
  def containsOnlyVowelsOrOnlyConsonants(): Boolean
}

// implementazione LettersHand
case class LettersHandImpl(_hand: ArrayBuffer[Card]) extends LettersHand{
  override def hand: ArrayBuffer[Card] = _hand
  override def playLetter(cardPosition: Int): Card = hand.remove(cardPosition)
  override def putLetter(cardPosition:Int, card: Card): Unit = hand.insert(cardPosition, card)
  override def changeHand(newHand: ArrayBuffer[Card]): Unit = {
    hand.clear()
    hand.insertAll(0, newHand)
  }
  override def containsOnlyVowelsOrOnlyConsonants(): Boolean = {
    val vowels = Set("A", "E", "I", "O", "U")
    hand.forall(card => vowels.contains(card.letter)) || hand.forall(card => !vowels.contains(card.letter))
  }
  override def calculateHandPoint: Int = {
    var handValue = 0
    _hand.foreach(card => handValue += card.score)
    handValue
  }
}