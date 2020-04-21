package client.view

import java.util.{Timer, TimerTask}

import client.controller.Controller
import client.controller.Messages.ViewToClientMessages.UserMadeHisMove
import eu.hansolo.medusa.Gauge.SkinType
import eu.hansolo.medusa.{Gauge, GaugeBuilder}
import scalafx.application.Platform
import scalafx.geometry.Pos
import scalafx.scene.layout.GridPane
import scalafx.scene.paint.Color
import scalafx.scene.shape.Circle
import shared.Move.TimeOut

class TimerPanel() extends GridPane {
  private val WIDTH = 260
  private val HEIGHT = 125
  private val TIMER_MAX_VALUE = 120
  private val UPDATE_TIME = 100
  prefWidth = WIDTH
  prefHeight = HEIGHT
  alignment = Pos.Center
  val stopwatch = new Stopwatch
  val circle: Circle = new Circle
  var progress: Gauge = GaugeBuilder.create()
    .skinType(SkinType.BAR)
    .decimals(0)
    .maxValue(TIMER_MAX_VALUE)
    .minValue(0)
    .barColor(Color.Black)
    .valueColor(Color.Black)
    .build()

  add(progress, 0, 0)

  def startTurn(): Unit = {
    val timer = new Timer()
    timer.scheduleAtFixedRate(new TimerTask {
      override def run(): Unit = {
        if (!stopwatch.paused) {
          if (stopwatch.getSeconds <= 0) {
            Platform.runLater(() => {
              progress.setValue(0)
            })
            timer.cancel()
            if (Controller.isMyTurn) {
              Controller.collectLetters()
              View.sendToClient(UserMadeHisMove(TimeOut()))
            }
          } else {
            Platform.runLater(() => {
              progress.setValue(stopwatch.getSeconds)
            })
          }
        }
      }
    }, UPDATE_TIME, UPDATE_TIME)
  }

  def pauseTimer(): Unit = {
    stopwatch.pause()
  }

  def resumeTimer(): Unit = {
    stopwatch.resume()
  }

  def restartTimer() : Unit = {
    stopwatch.restart()
  }
}
