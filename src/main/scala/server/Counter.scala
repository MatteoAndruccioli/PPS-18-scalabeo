package server

/** Fornisce un modello per un contatore
 */
trait Counter {
  def increment() : Unit
  def isFull : Boolean
  def reset() : Unit
  }

/**Implementazione di un contatore
 * @param maxValue specifica quale è il valore massimo che il contatore assume per essere considerato saturo
 */
case class CounterImpl(maxValue: Int) extends Counter {

  private var value : Int = 0
  private val max : Int = maxValue


  /** Incrementa il contatore
   */
  override def increment(): Unit = this.value = this.value + 1

  /** Controlla se il contatore ha raggiunto il valore massimo
   * @return true se il contatore ha raggiunto il massimo prestabilito
   */
  override def isFull: Boolean = this.value == this.max

  /** Azzera il contatore
   */
  override def reset(): Unit = this.value = 0
}
