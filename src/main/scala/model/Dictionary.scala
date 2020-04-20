package model

import scala.io.Source
import scala.util.matching.Regex
import RegexUtils._

object RegexUtils {
  implicit class RichRegex(val underlying: Regex) extends AnyVal {
    def matches(s: String): Boolean = underlying.pattern.matcher(s).matches
  }
}

sealed trait Dictionary {
  def dictionaryPath: String
  def dictionarySet: Set[String]
  // metodo per il controllo di una lista di parole nel dizionario
  def checkWords(filter: List[String]): Boolean
}

class DictionaryImpl(val _dictionaryPath: String) extends Dictionary {
  override def dictionaryPath: String = _dictionaryPath
  override def dictionarySet: Set[String] = populateDictionary()
  // metodo per il caricamento del dizionario
  private def populateDictionary(): Set[String] = Source.fromInputStream(getClass.getResourceAsStream(dictionaryPath)).getLines().toSet
  // metodo per il controllo di una lista di parole nel dizionario
  override def checkWords(listToCheck: List[String]): Boolean = if(listToCheck.nonEmpty) listToCheck.forall(word => checkWord(word)) else false
  private def checkWord(filter: String): Boolean = dictionarySet.exists(dictionaryWord => filter.r matches dictionaryWord)
}
