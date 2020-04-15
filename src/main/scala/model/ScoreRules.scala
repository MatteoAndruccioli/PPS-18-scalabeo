package model

import model.Card
import scala.collection.mutable.ArrayBuffer


package object scoreConstants {

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
  val scarabeoWord = "scarabeo"
}

package object scoreRules{

  // regola assegnazione valore moltiplicatore del punteggio della parola
  def wordMultiplier(positionBonus: String): Int = positionBonus match{
    case scoreConstants.wordForTwo => 2
    case scoreConstants.wordForThree => 3
    case _ => 0
  }

  // regola assegnazione valore moltiplicatore del punteggio della lettera
  def letterMultiplier(positionBonus: String): Int = positionBonus match{
    case scoreConstants.letterForTwo => 2
    case scoreConstants.letterForThree => 3
    case _ => 1
  }

  // regola assegnazione bonus lunghezza della parola inserita
  def lenghtBonus(word: ArrayBuffer[(Card, String)]) : Int = word.length match{
    case 8 => scoreConstants.bonusLenght8 + noScarabeoCardBonus(word)
    case 7 => scoreConstants.bonusLenght7 + noScarabeoCardBonus(word)
    case 6 => scoreConstants.bonusLenght6 + noScarabeoCardBonus(word)
    case _ => 0
  }

  // regola assegnazione bonus per la composizione della parola senza scarabeo
  def noScarabeoCardBonus(word: ArrayBuffer[(Card, String)]): Int =
    if (word exists (tuple => tuple._1.letter == scoreConstants.scarabeo)) 0 else scoreConstants.bonusWithoutScarabeo

  // regola assegnazione bonus per parola formata == SCARABEO
  def wordScarabeoBonus(word: ArrayBuffer[(Card, String)]): Int =  if ((for (tuple <- word; playedWord <- tuple._1.letter) yield playedWord).mkString("").toLowerCase.equals(scoreConstants.scarabeoWord)) scoreConstants.bonusScarabeoWord else 0

  // regola assegnazione bonus per la prima parola composta nella partita
  def bonusFirstWord(): Int = scoreConstants.firstWordBonus
}