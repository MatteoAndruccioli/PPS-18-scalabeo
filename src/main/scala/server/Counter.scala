package server


trait Counter {
  def increment() : Unit
  def isFull() : Boolean
  def reset() : Unit
  }

case class CounterImpl(maxValue: Int) extends Counter {

  private var value : Int = 0
  private val max : Int = maxValue

  override def increment(): Unit = this.value = this.value + 1

  override def isFull(): Boolean = this.value == this.max

  override def reset(): Unit = this.value = 0
}
