package client.view

import client.controller.{Controller, GameManager}
import model.CardImpl
import scalafx.application.JFXApp


object LaunchTest extends JFXApp {
  //View.onMatchStart()
  //Controller.userTurnBegins()
  GameManager.newGame(Vector(CardImpl("s"), CardImpl("c"), CardImpl("a"), CardImpl("l"), CardImpl("a"), CardImpl("b"), CardImpl("e"), CardImpl("o")))
}
