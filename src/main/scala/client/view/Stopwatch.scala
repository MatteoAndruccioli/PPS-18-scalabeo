package client.view

class Stopwatch {
  private val INITIAL_TIME_MS = 120000
  var start: Long = System.currentTimeMillis()
  def elapsed: Long = System.currentTimeMillis() - start
  var paused: Boolean = false
  var time: Float = INITIAL_TIME_MS

  def restart(): Unit = {
    start = System.currentTimeMillis()
    time = INITIAL_TIME_MS
    paused = false
  }

  def pause(): Unit = {
    paused = true
    time = ((time - elapsed))
  }

  def resume(): Unit = {
    start = System.currentTimeMillis()
    paused = false
  }

  def getSeconds: Float = if (paused) time else (time - elapsed)/1000
}