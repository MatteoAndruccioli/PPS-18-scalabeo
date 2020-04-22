package model

import scala.util.Random

package object cardConstants {
  // lista del punteggio e della cardinalità che le lettere devono avere in una partita
  // (lettare. valore, cardinalità)
  val lettersScoresCardinalities = List(("A",1,12), ("B",4,4), ("C",1,7), ("D",4,4),
    ("E",1,12), ("F",4,4), ("G",4,4), ("H",8,2),
    ("I",1,12), ("L",2,6), ("M",2,6), ("N",2,6),
    ("O",1,12), ("P",3,4), ("Q",10,2), ("R",1,7),
    ("S",1,7),  ("T",1,7), ("U",4,4), ("V",4,4),
    ("Z",8,2),  ("[a-zA-Z]",1,2))
  // lista utilizzata per i test di fine partita
  val lettersScoresCardinalitiesTest = List(("I",1,8), ("S",1,8))

  // valore della card di default
  val defaultCard = CardImpl("NULL")
}

// Carta: lettera e relativo valore
sealed trait Card{
  def letter: String
  def score: Int
}

// implementazione della Carta
case class CardImpl (var _letter : String) extends Card {
  override def letter: String = _letter
  // lo score è assegnato automaticamente usando la lista definita in lettersScoresCardinalities
  override def score: Int = cardConstants.lettersScoresCardinalities.find(s => s._1 == _letter).head._2
}


// Sacchetto contentenente le lettere da poter usare durante una partita
sealed trait LettersBag {
  def test: Boolean
  def bag: List[Card]
  // metodo per estarre un numero di lettere dalla bag
  def takeRandomElementFromBagOfLetters(lettersToTake: Int): Option[List[Card]]
  // metodo per inserire una lettera nella bag
  def reinsertCardInBag(cardsToInsert: Vector[Card]): Unit
}

// implementazione LettersBag
case class LettersBagImpl(test: Boolean= false) extends LettersBag {
  private var _bag: List[Card] = List()
  if (test) _bag = populateBag(cardConstants.lettersScoresCardinalitiesTest) else _bag = populateBag(cardConstants.lettersScoresCardinalities)
  private def populateBag(list: List[(String, Int, Int)]): List[Card] = list.flatMap(tuple2Cards)
  private def tuple2Cards(tuple: (String, Int, Int)): List[Card] = List.fill(tuple._3)(CardImpl(tuple._1))
  override def bag: List[Card] = _bag
  // metodo per estarre un numero di lettere dalla bag
  def takeRandomElementFromBagOfLetters(lettersToTake: Int): Option[List[Card]] = _bag match {
    case Nil => Option.empty
    case _ if lettersToTake > _bag.length =>
      val shuffledList = Random.shuffle(_bag)
      _bag = Nil
      Some(shuffledList)
    case _ =>
      val shuffledList = Random.shuffle(_bag)
      _bag = shuffledList.drop(lettersToTake)
      Some(shuffledList.slice(0, lettersToTake))
  }
  // metodo per inserire una lettera nella bag
  override def reinsertCardInBag(cardsToInsert: Vector[Card]): Unit = _bag = _bag ++ cardsToInsert
}


// mano delle card di ogni giocatore
sealed trait LettersHand {
  def hand: Vector[Card]
  // metodo per giocare una lettera dalla mano
  def playLetter (cardPosition: Int): Card
  // metodo per inserire una lettera nella mano
  def putLetter (cardPosition: Int, card: Card)
  // metodo per il cambio di una mano
  def changeHand(newHand: Vector[Card])
  // metodo per il calcolo dei punti della mano
  def calculateHandPoint: Int
  // metodo per controllare se la mano contiene solo vocali o constanti
  def containsOnlyVowelsOrOnlyConsonants(): Boolean
}

// implementazione LettersHand
case class LettersHandImpl(firstHand: Vector[Card]) extends LettersHand{
  private var _hand = firstHand
  override def hand: Vector[Card] = _hand
  // metodo per giocare una lettera dalla mano
  override def playLetter(cardPosition: Int): Card = {
    val card = hand(cardPosition)
    _hand = _hand.patch(cardPosition, Nil, 1)
    card
  }
  // metodo per inserire una lettera nella mano
  override def putLetter(cardPosition:Int, card: Card): Unit = _hand = _hand.updated(cardPosition, card)
  // metodo per il cambio di una mano
  override def changeHand(newHand: Vector[Card]): Unit = _hand = newHand
  // metodo per il calcolo dei punti della mano
  override def containsOnlyVowelsOrOnlyConsonants(): Boolean = {
    val vowels = Set("A", "E", "I", "O", "U")
    hand.forall(card => vowels.contains(card.letter)) || hand.forall(card => !vowels.contains(card.letter))
  }
  // metodo per controllare se la mano contiene solo vocali o constanti
  override def calculateHandPoint: Int = firstHand.foldLeft(0)(_+_.score)
}
