package server.dictionary

object DictionaryUtils {

  import scala.io.Source

  //permette di ottenere una lista di parole contenute in un file txt
  def uploadDictionary(filename: String): Set[String] = {
    val fileStream = getClass.getResourceAsStream(filename)
    val lines = Source.fromInputStream(fileStream).getLines
    val linesSet = lines.toSet
    linesSet
  }
}
