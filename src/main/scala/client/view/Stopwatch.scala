package client.view

class Stopwatch {
  var start: Long = System.currentTimeMillis()
  def elapsed: Long = System.currentTimeMillis() - start
  var paused: Boolean = false
  var time: Float = 120000

  def restart(): Unit = {
    start = System.currentTimeMillis()
    time = 120000
    paused = false
  }

  def pause(): Unit = {
    paused = true
    time = ((time - elapsed))
    println("Pausato il timer a: " + time)
  }

  def resume(): Unit = {
    println("Il timer riparte da: " + time )
    start = System.currentTimeMillis()
    println(elapsed)
    paused = false
  }

  def getSeconds: Float = if (paused) time else (time - elapsed)/1000
}