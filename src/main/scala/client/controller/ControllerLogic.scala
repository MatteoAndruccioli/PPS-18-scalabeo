package client.controller

import client.controller.Messages.ViewToClientMessages.UserMadeHisMove
import client.controller.MoveOutcome.{AcceptedWord, HandSwitchAccepted, HandSwitchRefused, PassReceived, RefusedWord, ServerDown, TimeoutReceived}
import client.controller.MoveOutcome.ServerDown.{GameServerDown, GreetingServerDown}
import client.view.{BoardInteraction, LetterStatus, LetterTile, View}
import model.{BoardTile, Card}
import shared.Move.WordMove

trait ControllerLogic {
  def startGui(): Unit
  def onLoginResponse(): Unit
  def askUserToJoinGame(): Unit
  def onMatchStart(hand:Vector[Card], players: List[String]): Unit
  def userTurnBegins(): Unit
  def turnEndUpdates(ranking: List[(String,Int)], board:List[BoardTile]): Unit
  def addCardToTile(position: Int, x: Int, y: Int): Unit
  def collectLetters(): Unit
  def playWord(): Unit
  def moveOutcome[A >: MoveOutcome](outcome: A):Unit
  def updateHand(hand:Vector[Card]): Unit
  def takeLettersBackInHand(): Unit
  def userTurnContinues(): Unit
  def isMulliganAvailable: Boolean
  def onConnectionFailed(): Unit
  def serversDown(server: ServerDown):Unit
  def matchEnded(player: String, playerWon:Boolean): Unit
  def playerLeft(): Unit
  def terminate(): Unit
  def showInChat(sender: String, message: String): Unit
  def condPrintln(verbose:Boolean)(x: Any): Unit = if (verbose) println(x)
}

object ControllerLogic {
  //implementazione vera e propria di ControllerLogic
  case class CleverLogic() extends ControllerLogic {
    def startGui(): Unit = {
      new Thread(() => {
        View.main(Array[String]())
      }).start()
    }

    def onLoginResponse(): Unit = {
      View.onLoginResponse()
    }

    def askUserToJoinGame(): Unit = {
      View.askUserToJoinGame()
    }

    def onMatchStart(hand:Vector[Card], players: List[String]): Unit = {
      View.onMatchStart(hand.map(c => (c.letter, c.score)), players)
      GameManager.newGame(hand)
    }

    def userTurnBegins(): Unit = {
      View.userTurnBegins()
    }

    def turnEndUpdates(ranking: List[(String,Int)], board:List[BoardTile]): Unit = {
      View.updateLeaderboard(ranking)
      GameManager.addPlayedWordAndConfirm(board)
      View.turnEndUpdates(board.map(b => (LetterTile(60, b.card.letter, b.card.score.toString, 0, LetterStatus.insertedConfirmed), b.position.row, b.position.col)))
    }

    def addCardToTile(position: Int, x: Int, y: Int): Unit = {
      GameManager.addCardToTile(position, x, y)
    }

    def collectLetters(): Unit = {
      GameManager.collectLetters()
      BoardInteraction.collectLetters()
    }

    def playWord(): Unit = {
      Controller.endMyTurn()
      val playedWord = GameManager.getPlayedWord
      if(playedWord.nonEmpty) {
        playedWord.foreach(b => {
          print(b.card.letter)
        })
        Controller.sendToClient(UserMadeHisMove(WordMove(playedWord)))
      } else {
        View.showEventMessage("Devi inserire almeno una lettera per inviare la tua mossa")
        Controller.userTurnContinues()
      }
    }

    //metodo attraverso cui il Client comunica al controller l'esito della mossa inviata al GameServer
    def moveOutcome[A >: MoveOutcome](outcome: A):Unit = outcome match {
      case _: RefusedWord => takeLettersBackInHand(); userTurnContinues()
      case _: HandSwitchRefused => userTurnContinues()
      case _: AcceptedWord => updateHand(outcome.asInstanceOf[AcceptedWord].hand); View.confirmPlay(); GameManager.confirmPlay()
      case _: HandSwitchAccepted => updateHand(outcome.asInstanceOf[HandSwitchAccepted].hand); Controller.endMyTurn()
      case _: PassReceived => Controller.endMyTurn()
      case _: TimeoutReceived => Controller.endMyTurn()
    }

    def updateHand(hand:Vector[Card]): Unit = {
      View.updateHand(hand.map(c => (c.letter, c.score)))
      GameManager.changeHand(hand)
    }

    def takeLettersBackInHand(): Unit = {
      View.getLettersBackFromBoard()
      GameManager.collectLetters()
    }

    def userTurnContinues(): Unit = {
      Controller.setMyTurn()
      View.userTurnContinues()
    }

    def showInChat(sender: String, message: String): Unit = {
      View.showInChat(sender, message)
    }

    def isMulliganAvailable: Boolean = {
      GameManager.isMulliganAvailable()
    }

    def onConnectionFailed(): Unit = {
      View.terminate()
    }

    def serversDown(server: ServerDown):Unit = {
      server match {
        case _: GreetingServerDown => View.greetingDisconnected()
        case _: GameServerDown => View.gameServerDisconnected()
      }
    }

    def matchEnded(player: String, playerWon:Boolean): Unit =  {
      Controller.endMyTurn()
      BoardInteraction.reset()
      View.matchEnded(player, playerWon)
    }

    def playerLeft(): Unit = {
      View.playerLeft()
    }

    def terminate(): Unit = {
      View.terminate()
    }
  }

  //E' una vesione dummy di implementazione di ControllerLogic utile in fase di test e debug
  class DummyLogic(verbose: Boolean = true) extends ControllerLogic{

    override def startGui(): Unit = myPrintln("invocato startGui()")

    override def onLoginResponse(): Unit = myPrintln("invocato onLoginResponse()")

    override def askUserToJoinGame(): Unit = myPrintln("invocato askUserToJoinGame()")

    override def onMatchStart(hand: Vector[Card], players: List[String]): Unit = {
      myPrintln("invocato askUserToJoinGame(hand: Vector[Card], players: List[String])")
      myPrintln("Hand: ")
      hand.foreach(c=>myPrintln(c.toString))
      myPrintln("Players: ")
      myPrintln(players)
    }

    override def userTurnBegins(): Unit = myPrintln("invocato userTurnBegins()")

    override def turnEndUpdates(ranking: List[(String, Int)], board: List[BoardTile]): Unit = {
      myPrintln("invocato turnEndUpdates(ranking: List[(String, Int)], board: List[BoardTile])")
      myPrintln("ranking: ")
      ranking.foreach(c=>myPrintln("\n (" + c._1 + "," + c._2 +")"))
      myPrintln("board: ")
      board.foreach(c=>myPrintln("\n" + c.toString))
    }

    override def addCardToTile(position: Int, x: Int, y: Int): Unit = {
      myPrintln("invocato addCardToTile(position: Int, x: Int, y: Int)")
      myPrintln("position: " + position)
      myPrintln("x: "+ x)
      myPrintln("y: "+ y)
    }

    override def collectLetters(): Unit = myPrintln("invocato collectLetters()")

    override def playWord(): Unit = myPrintln("invocato playWord()")

    override def moveOutcome[A >: MoveOutcome](outcome: A): Unit = {
      myPrintln("invocato moveOutcome[A >: MoveOutcome](outcome: A)")
      myPrintln("outcome: " + outcome)
    }

    override def updateHand(hand: Vector[Card]): Unit = {
      myPrintln("invocato updateHand(hand: Vector[Card])")
      myPrintln("Hand: ")
      hand.foreach(c=>myPrintln(c.toString))
    }

    override def takeLettersBackInHand(): Unit = myPrintln("invocato takeLettersBackInHand()")

    override def userTurnContinues(): Unit = myPrintln("invocato userTurnContinues()")

    override def isMulliganAvailable: Boolean = {
      myPrintln("invocato isMulliganAvailable()")
      true
    }

    override def onConnectionFailed(): Unit = myPrintln("invocato onConnectionFailed()")

    override def serversDown(server: ServerDown): Unit = {
      myPrintln("invocato serversDown(server: ServerDown)")
      myPrintln("server: " + server.toString)
    }

    override def matchEnded(player: String, playerWon: Boolean): Unit = {
      myPrintln("invocato matchEnded(player: String, playerWon: Boolean)")
      myPrintln("player: " + player)
      myPrintln("playerWon: " + playerWon)
    }

    override def playerLeft(): Unit = myPrintln("invocato playerLeft()")

    override def terminate(): Unit = myPrintln("invocato terminate()")

    override def showInChat(sender: String, message: String): Unit = {
      myPrintln("invocato showInChat(sender: String, message: String)")
      myPrintln("sender: " + sender)
      myPrintln("message: " + message)
    }

    def myPrintln:Any => Unit = condPrintln(verbose)
  }
}
