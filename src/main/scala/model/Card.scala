package model

package object constants {
  // lista del punteggio e della cardinalità che le lettere devono avere in una partita
  // (lettare. valore, cardinalità)
  val lettersScoresCardinalities = List(("A",1,12), ("B",4,4), ("C",1,7), ("D",4,4),
    ("E",1,12), ("F",4,4), ("G",4,4), ("H",8,2),
    ("I",1,12), ("L",2,6), ("M",2,6), ("N",2,6),
    ("O",1,12), ("P",3,4), ("Q",10,2), ("R",1,7),
    ("S",1,7),  ("T",1,7), ("U",4,4), ("V",4,4),
    ("Z",8,2),  ("[a-zA-Z]",1,2))

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

