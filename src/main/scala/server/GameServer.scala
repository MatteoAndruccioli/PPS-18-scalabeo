package server

import shared.Topic.GAME_SERVER_SEND_TOPIC
import server.GreetingToGameServer.{EndGameToGreetingAck, InitGame}
import akka.actor.{Actor, ActorRef}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import model._
import server.GameServerToGameServer.EndGameInit
import server.GameServerToGreeting.EndGameToGreeting
import shared.ClientMoveAckType._
import shared.ClientToGameServerMessages.{ClientMadeMove, DisconnectionToGameServerNotification, EndTurnUpdateAck, GameEndedAck, MatchTopicListenAck, PlayerTurnBeginAck, SomeoneDisconnectedAck}
import shared.GameServerToClientMessages.{ClientMoveAck, DisconnectionToGameServerNotificationAck, EndTurnUpdate, GameEnded, MatchTopicListenQuery, PlayerTurnBegins, SomeoneDisconnected}
import shared.{ClusterScheduler, CustomScheduler, Move}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class GameServer(players : List[ActorRef], mapUsername : Map[ActorRef, String]) extends Actor {

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(GAME_SERVER_SEND_TOPIC, self)
  private val cluster = Cluster.get(context.system)
  private val scheduler: CustomScheduler = ClusterScheduler(cluster)
  private val nPlayer = players.size

  private var greetingServerRef: ActorRef = _
  private val gamePlayers = players
  private val gamePlayersUsername: Map[ActorRef, String] = mapUsername

  private val board = BoardImpl()
  private val pouch = LettersBagImpl()
  private var playersHand = mutable.Map[ActorRef, LettersHandImpl]()
  //creo le mani
  gamePlayers.foreach(p => playersHand+=(p -> LettersHandImpl.apply(mutable.ArrayBuffer(pouch.takeRandomElementFromBagOfLetters(8).get : _*))))
  private var playedWord : ArrayBuffer[BoardTile] = ArrayBuffer[BoardTile]()
  private var numberOfPlayedTileInHand = 0
  //creo il dizionario
  private val dictionaryPath: String = "/dictionary/dictionary.txt"
  private val dictionary: DictionaryImpl = new DictionaryImpl(dictionaryPath)

  private val ranking : Ranking = new RankingImpl(players)

  private var turn = 0

  //variabili ack
  private var ackTopicReceived = CounterImpl(nPlayer)
  private var ackTurn = CounterImpl(nPlayer)
  private var ackEndTurn = CounterImpl(nPlayer)
  private var ackEndGame = CounterImpl(nPlayer)
  private var ackDisconnection = CounterImpl(nPlayer)

  override def receive: Receive = {
    case _: InitGame =>
      scheduler.replaceBehaviourAndStart(() => sendTopic())
      greetingServerRef = sender()
    case _: MatchTopicListenAck =>
      ackTopicReceived.increment()
      if (ackTopicReceived.isFull()) {
        scheduler.stopTask()
        ackTopicReceived.reset()
        scheduler.replaceBehaviourAndStart(() => sendTurn())
        println("STA A" + gamePlayers(turn).toString() + " IL CUI TURNO è = " + turn)
      }
    case _: PlayerTurnBeginAck =>
      ackTurn.increment()
      if (ackTurn.isFull()) {
        scheduler.stopTask()
        ackTurn.reset()
      }
    case message: ClientMadeMove => message.move match {
      case _: Move.Pass =>
        if (sender().equals(gamePlayers(turn))) {
          sender ! ClientMoveAck(PassAck())
          println("IL player " + sender() + " ha passato")
          scheduler.replaceBehaviourAndStart(() => sendUpdate())
        }
      case _: Move.TimeOut =>
        if (sender().equals(gamePlayers(turn))) {
          sender ! ClientMoveAck(TimeoutAck())
          println("IL player " + sender() + " ha fatto passare troppo tempo: TIMEOUT")
          scheduler.replaceBehaviourAndStart(() => sendUpdate())
        }
      case _: Move.Switch =>
        if(sender().equals(gamePlayers(turn))){
          if (playersHand(sender()).containsOnlyVowelsOrOnlyConsonants()){
            val nCard = playersHand(sender())._hand.size
            pouch.reinsertCardInBag(playersHand(sender())._hand)
            playersHand(sender()) = LettersHandImpl.apply(mutable.ArrayBuffer(pouch.takeRandomElementFromBagOfLetters(nCard).get : _*))
            sender ! ClientMoveAck(HandSwitchRequestAccepted(playersHand(sender())._hand))
            scheduler.replaceBehaviourAndStart(() => sendUpdate())
          } else {
            sender ! ClientMoveAck(HandSwitchRequestRefused())
          }
        }
      case message: Move.WordMove =>
        if(sender().equals(gamePlayers(turn))){
          message.word.foreach(boardTile => playedWord.insert(0,boardTile))
          println("RICEVUTA MOSSA: "+ playedWord.toString +" da "+ sender())
          board.addPlayedWord(List.concat(playedWord))
          println(board.boardTiles.toString)
          //inserire check validità parole in futuro
          if(board.checkGoodWordDirection() && dictionary.checkWords(board.getWordsFromLetters(board.takeCardToCalculatePoints()))) {
            ranking.updatePoints(sender(),board.calculateTurnPoints(board.takeCardToCalculatePoints()))
            replaceHand()
            sender ! ClientMoveAck(WordAccepted(playersHand(sender())._hand))
            //controllo se è finito il game
            if(playersHand(sender()).hand.isEmpty && pouch.bag.isEmpty){
              //toglieere i punti a tutti per le tessere nella propria mano e aggiungerli al vincitore
              for(player <- gamePlayers) {
                ranking.removePoints(player, playersHand(player).calculateHandPoint)
                ranking.updatePoints(sender(), playersHand(player).calculateHandPoint)
              }
              context.become(EndGame)
              self ! EndGameInit()
            }
            scheduler.replaceBehaviourAndStart(() => sendUpdate())
          } else {
            board.clearBoardFromPlayedWords()
            sender ! ClientMoveAck(WordRefused())
          }
          playedWord.clear()
        }

      case _: EndTurnUpdateAck =>
        ackEndTurn.increment()
        if (ackEndTurn.isFull()) {
          scheduler.stopTask()
          ackEndTurn.reset()
          incrementTurn()
          scheduler.replaceBehaviourAndStart(() => sendTurn())
          println("STA A " + gamePlayers(turn).toString() + " IL CUI TURNO è = " + turn)
        }

      //gestione disconnessione
      case _ : DisconnectionToGameServerNotification =>
        sender ! DisconnectionToGameServerNotificationAck()
        scheduler.stopTask()
        ackDisconnection.increment()
        scheduler.replaceBehaviourAndStart(() => sendDisconnection())

      case _: SomeoneDisconnectedAck =>
        ackDisconnection.increment()
        if (ackDisconnection.isFull()) {
          scheduler.stopTask()
          ackDisconnection.reset()
          scheduler.replaceBehaviourAndStart(()=>greetingServerRef ! EndGameToGreeting())
        }

      case _ : EndGameToGreetingAck =>
        scheduler.stopTask()
        context.stop(self)
    }
  }

  def EndGame : Receive = {
    case _ : EndGameInit =>
      scheduler.replaceBehaviourAndStart(() => mediator ! Publish(GAME_SERVER_SEND_TOPIC,GameEnded(sender(),mapUsername(sender()))))
    case  _ : GameEndedAck =>
      ackEndGame.increment()
      if (ackEndGame.isFull()) {
        ackEndGame.reset()
        scheduler.stopTask()
        scheduler.replaceBehaviourAndStart(()=>greetingServerRef ! EndGameToGreeting())
      }
    //se qualcuno si disconnette ora lo considero come un ack
    case _ : DisconnectionToGameServerNotification =>
      ackEndGame.increment()
      if (ackEndGame.isFull()) {
        ackEndGame.reset()
        scheduler.stopTask()
        scheduler.replaceBehaviourAndStart(()=>greetingServerRef ! EndGameToGreeting())
      }
    case _ : EndGameToGreetingAck =>
      scheduler.stopTask()
      context.stop(self)
  }

  //comportamento dello scheduler
  private def sendTopic(): Unit = {
    gamePlayers.foreach(player => player ! MatchTopicListenQuery(GAME_SERVER_SEND_TOPIC, playersHand(player)._hand, gamePlayersUsername.values.toList))
  }

  private def sendTurn(): Unit = {
    mediator ! Publish(GAME_SERVER_SEND_TOPIC, PlayerTurnBegins(gamePlayers(turn)))
  }

  private def sendUpdate(): Unit = {
    val rankingTuples : ListBuffer[(String, Int)] = ListBuffer()
    for( player <- gamePlayers){
      rankingTuples.insert(0,(gamePlayersUsername(player), ranking.ranking(player)))
    }
    mediator ! Publish(GAME_SERVER_SEND_TOPIC, EndTurnUpdate(rankingTuples.toList, board.playedWord))
  }

  private def sendDisconnection(): Unit = {
    mediator ! Publish(GAME_SERVER_SEND_TOPIC, SomeoneDisconnected())
  }

  //metodi utilità
  private def incrementTurn(){
    turn = turn +1
  }
  private def replaceHand() : Unit = {
    for(boardTile <- playedWord){
      playersHand(sender()).playLetter(playersHand(sender())._hand.indexOf(CardImpl(boardTile.card.letter)))
      numberOfPlayedTileInHand = numberOfPlayedTileInHand + 1
    }
    val drawnTiles = pouch.takeRandomElementFromBagOfLetters(numberOfPlayedTileInHand).getOrElse(List())
    for(i <- drawnTiles.indices) playersHand(sender()).putLetter(playersHand(sender()).hand.size,CardImpl(drawnTiles(i).letter))
    numberOfPlayedTileInHand = 0
  }
}