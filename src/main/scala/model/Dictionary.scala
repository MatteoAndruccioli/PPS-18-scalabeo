package model

import scala.io.Source
import scala.util.matching.Regex
import RegexUtils._

/** classe per le espressioni regolari */
object RegexUtils {
  implicit class RichRegex(val underlying: Regex) extends AnyVal {
    def matches(s: String): Boolean = underlying.pattern.matcher(s).matches
  }
}

/** Dizionario delle parole della lingua italiana
  * - dictionaryPath: percorso dove poter trovare il file del dizionario
  * - dictionarySet: il dizionario
  * - checkWords: metodo per il controllo di una lista di parole nel dizionario
  */
sealed trait Dictionary {
  def dictionaryPath: String
  def dictionarySet: Set[String]
  def checkWords(filter: List[String]): Boolean
}

/** Implementazione del dizionario delle parole della lingua italiana
  * @param _dictionaryPath: percorso dove poter trovare il file del dizionario
  */
class DictionaryImpl(val _dictionaryPath: String) extends Dictionary {
  /** metodo per accedere al percorso del dizionario
    * @return
    */
  override def dictionaryPath: String = _dictionaryPath
  /** metodo per accedere al dizionario
    * @return dizionario
    */
  override def dictionarySet: Set[String] = populateDictionary()
  private def populateDictionary(): Set[String] = Source.fromInputStream(getClass.getResourceAsStream(dictionaryPath)).getLines().toSet
  //
  /** metodo per il controllo di una lista di parole nel dizionario
    * @param listToCheck: lista delle parole da controllare
    * @return: vero se tutte le parole appartengono al dizionario, falso altrimenti
    */
  override def checkWords(listToCheck: List[String]): Boolean = if(listToCheck.nonEmpty) listToCheck.forall(word => checkWord(word)) else false
  private def checkWord(filter: String): Boolean = dictionarySet.exists(dictionaryWord => filter.r matches dictionaryWord)
}
