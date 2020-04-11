package model

import org.scalatest._

class DictionaryTest extends FlatSpec {

  "A list of words " should " be checked" in {
    val wordsToCheck = List("ago","abaco")
    val dictionary: DictionaryImpl = new DictionaryImpl("/dictionary/dictionary.txt")
    assert(dictionary.checkWords(wordsToCheck))
  }

  "A wrong words " should " be find" in {
    val wordsToCheck = List("arstgo")
    val dictionary: DictionaryImpl = new DictionaryImpl("/dictionary/dictionary.txt")
    assert(!dictionary.checkWords(wordsToCheck))
  }

  "A word with a scarabeo" should " be checked" in {
    val wordsToCheck = List("a[a-zA-Z]o")
    val dictionary: DictionaryImpl = new DictionaryImpl("/dictionary/dictionary.txt")
    assert(dictionary.checkWords(wordsToCheck))
  }

  "A word with no letters" should "not be checked" in {
    val wordsToCheck = List()
    val dictionary: DictionaryImpl = new DictionaryImpl("/dictionary/dictionary.txt")
    assert(!dictionary.checkWords(wordsToCheck))
  }

}
