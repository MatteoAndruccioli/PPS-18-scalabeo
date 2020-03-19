package server.dictionary

object Dictionary {

  import DictionaryUtils._

  val DICTIONARY_FILE_NAME: String = "/dictionary/dictionary.txt"

  var dictionary: Set[String] = uploadDictionary(DICTIONARY_FILE_NAME)

  //permette di indicare una lista di stringhe come nuovo dizionario
  def replaceDefaultDictionary(set: Set[String]): Unit = {
    dictionary = set
  }

  //permette di indicare un file txt da cui caricare il dizionario
  //il file txt deve contenere tutte parole ognune su una linea diversa
  def replaceDefaultDictionary(databasePath: String): Unit = {
    dictionary = uploadDictionary(databasePath)
  }
}
