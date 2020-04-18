package model

import org.scalatest._

class DictionaryTest extends FlatSpec {
  "A check " should " be performed on words validity: correct words" in {
    val wordsToCheck = List("ago","abaco")
    val dictionary: DictionaryImpl = new DictionaryImpl("/dictionary/dictionary.txt")
    assert(dictionary.checkWords(wordsToCheck))
  }
  "A check " should " be performed on word validity: misspelled word" in {
    val wordsToCheck = List("arstgo")
    val dictionary: DictionaryImpl = new DictionaryImpl("/dictionary/dictionary.txt")
    assert(!dictionary.checkWords(wordsToCheck))
  }
  "A check " should " be performed on word validity: scarabeo tile in the word" in {
    val wordsToCheck = List("a[a-zA-Z]o")
    val dictionary: DictionaryImpl = new DictionaryImpl("/dictionary/dictionary.txt")
    assert(dictionary.checkWords(wordsToCheck))
  }
  "A word with no letters" should " not be checked" in {
    val wordsToCheck = List()
    val dictionary: DictionaryImpl = new DictionaryImpl("/dictionary/dictionary.txt")
    assert(!dictionary.checkWords(wordsToCheck))
  }
}
