package client.view

/** Classe che modella il modello di un timer.
 *
 */
class Stopwatch {
  private val INITIAL_TIME_MS = 120000
  var start: Long = System.currentTimeMillis()
  def elapsed: Long = System.currentTimeMillis() - start
  var paused: Boolean = false
  var time: Float = INITIAL_TIME_MS

  /** Restarta il timer dall'inizio.
   *
   */
  def restart(): Unit = {
    start = System.currentTimeMillis()
    time = INITIAL_TIME_MS
    paused = false
  }

  /** Ferma il timer in un momento preciso.
   *
   */
  def pause(): Unit = {
    paused = true
    time = ((time - elapsed))
  }

  /** Il timer riprende da dove era stato messo in pausa.
   *
   */
  def resume(): Unit = {
    start = System.currentTimeMillis()
    paused = false
  }

  /** Metodo che ritorna il valore del timer in secondi.
   *
   * @return il valore del timer in secondi
   */
  def getSeconds: Float = if (paused) time else (time - elapsed)/1000
}