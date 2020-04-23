package model

import scala.util.Random

/** Costanti riguardanti la classe Card
  * - lettersScoreCardinalites: lista del punteggio e della cardinalità che le lettere devono avere in una partita
  *   (lettare. punteggio, cardinalità)
  * - defaultCard: card di default utilizzata nella Board per indicare una casella senza card
  */
package object cardConstants {

  val lettersScoresCardinalities = List(("A",1,12), ("B",4,4), ("C",1,7), ("D",4,4),
    ("E",1,12), ("F",4,4), ("G",4,4), ("H",8,2),
    ("I",1,12), ("L",2,6), ("M",2,6), ("N",2,6),
    ("O",1,12), ("P",3,4), ("Q",10,2), ("R",1,7),
    ("S",1,7),  ("T",1,7), ("U",4,4), ("V",4,4),
    ("Z",8,2),  ("[a-zA-Z]",1,2))

  val defaultCard = CardImpl("NULL")
}

/** Una tessera che può essere giocata
  * - letter: lettera che deve essera associata a questa tessera
  * - score: punteggio corrispondente alla tessera
  */
sealed trait Card{
  def letter: String
  def score: Int
}

/** Implementazione della Carta
  * @param _letter: lettera a cui deve corrispondere la Card
  */
case class CardImpl (var _letter : String) extends Card {
  override def letter: String = _letter

  /** Poter accedere al punteggio della tessera
    * corrisponde a quello definito in lettersScoresCardinalities
    * @return punteggio della lettera
    */
  override def score: Int = cardConstants.lettersScoresCardinalities.find(s => s._1 == _letter).head._2
}

/** Sacchetto contenente le lettere da estrarre durante la partita
  * - bag: il sacchetto delle lettere
  * - takeRandomElementFromBagOfLetters: metodo per estarre un numero di lettere dalla bag
  * - reinsertCardInBag: metodo per inserire una lettera nella bag
  */
sealed trait LettersBag {
  def bag: List[Card]
  def takeRandomElementFromBagOfLetters(lettersToTake: Int): Option[List[Card]]
  def reinsertCardInBag(cardsToInsert: Vector[Card]): Unit
}

/** Implementazione della sacchetto delle lettere */
case class LettersBagImpl() extends LettersBag {
  private var _bag: List[Card] = populateBag(cardConstants.lettersScoresCardinalities)
  private def populateBag(list: List[(String, Int, Int)]): List[Card] = list.flatMap(tuple => List.fill(tuple._3)(CardImpl(tuple._1)))

  /** metodo per accedere al sacchetto
    * @return il sacchetto delle lettere
    */
  override def bag: List[Card] = _bag
  /** metodo per estarre un numero di lettere dalla bag
    * @param lettersToTake : numero di lettere desiderate
    * @return lista delle n lettere estratte dal sacchetto
    */
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
  /** metodo per inserire una lettera nella bag
    * @param cardsToInsert: lettera da inserire nel sacchetto
    */
  override def reinsertCardInBag(cardsToInsert: Vector[Card]): Unit = _bag = _bag ++ cardsToInsert
}


/** Mano di lettere per un giocatore
  * - hand: la mano
  * - playLetter: giocare una lettera dalla mano
  * - putLetter: inserire una lettera nella mano
  * - changeHand: cambio della mano
  * - calculateHandPoint: calcolo del punteggio delle lettere nella mano
  * - containsOnlyVowelsOrOnlyConsonants: controllo se la mano contiene solo vocali o solo consonanti
  */
sealed trait LettersHand {
  def hand: Vector[Card]
  def playLetter (cardPosition: Int): Card
  def putLetter (cardPosition: Int, card: Card)
  def changeHand(newHand: Vector[Card])
  def calculateHandPoint: Int
  def containsOnlyVowelsOrOnlyConsonants(): Boolean
}

/** Implementazione della mano del giocatore
  * @param firstHand : mano iniziale del giocatore
  */
case class LettersHandImpl(firstHand: Vector[Card]) extends LettersHand{
  private var _hand = firstHand
  /** metodo per accedere alla mano
    * @return hand: la mano
    */
  override def hand: Vector[Card] = _hand
  /** metodo per giocare una lettera dalla mano
    * @param cardPosition: posizione della lettera da estrarre
    * @return la lettera voluta
    */
  override def playLetter(cardPosition: Int): Card = {
    val card = hand(cardPosition)
    _hand = _hand.patch(cardPosition, Nil, 1)
    card
  }
  /** metodo per inserire una lettera nella mano
    * @param cardPosition: posizione in cui inserire la lettera
    * @param card: lettera da inserire
    */
  override def putLetter(cardPosition:Int, card: Card): Unit =  _hand = _hand.slice(0,cardPosition)++Vector(card)++_hand.slice(cardPosition, _hand.length)
  /** metodo per il cambio di una mano
    * @param newHand: le nuove lettere da inserire
    */
  override def changeHand(newHand: Vector[Card]): Unit = _hand = newHand
  /** metodo per controllare se la mano contiene solo vocali o constanti
    * @return vero se contiene solo consonanti o solo vocali
    */
  override def containsOnlyVowelsOrOnlyConsonants(): Boolean = {
    val vowels = Set("A", "E", "I", "O", "U")
    hand.forall(card => vowels.contains(card.letter)) || hand.forall(card => !vowels.contains(card.letter))
  }
  /** metodo per il calcolo dei punti della mano
    * @return la somma dei punteggi delle lettere
    */
  override def calculateHandPoint: Int = firstHand.foldLeft(0)(_+_.score)
}
