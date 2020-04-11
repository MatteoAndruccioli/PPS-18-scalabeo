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
  prefWidth = 260
  prefHeight = 125
  alignment = Pos.Center
  val stopwatch = new Stopwatch
  val circle: Circle = new Circle
  var progress: Gauge = GaugeBuilder.create()
    .skinType(SkinType.BAR)
    .decimals(0)
    .maxValue(120)
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
              View.sendToClient(UserMadeHisMove(TimeOut()))
            }
          } else {
            Platform.runLater(() => {
              progress.setValue(stopwatch.getSeconds)
            })
          }
        }
      }
    }, 100, 100)
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
