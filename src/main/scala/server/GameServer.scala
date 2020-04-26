package server

import shared.Channels.{CHAT_TOPIC, GAME_SERVER_SEND_TOPIC}
import server.GreetingToGameServer.{EndGameToGreetingAck, InitGame}
import akka.actor.{Actor, ActorRef}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import model._
import server.GameServerToGameServer.EndGameInit
import server.GameServerToGreeting.EndGameToGreeting
import shared.ChatMessages.{SendChatMessageToGameServer, SendOnChat}
import shared.ClientMoveAckType._
import shared.ClientToGameServerMessages.{ClientMadeMove, DisconnectionToGameServerNotification, EndTurnUpdateAck, GameEndedAck, MatchTopicListenAck, PlayerTurnBeginAck, SomeoneDisconnectedAck}
import shared.GameServerToClientMessages.{ClientMoveAck, DisconnectionToGameServerNotificationAck, EndTurnUpdate, GameEnded, MatchTopicListenQuery, PlayerTurnBegins, SomeoneDisconnected}
import shared.{ClusterScheduler, CustomScheduler, Move}


/** GameServer è l'attore server che gestisce interamente una singola partita.
 *  GameServer è in grado di soddisfare ogni richiesta che può eseguire un client durante la partita:
 *    - gestisce la dinamica dei turni
 *    - valuta unicamente le giocate legittime dei giocatori
 *    - trasmette la chat della partita
 *    - contiene la classifica della partita
 *    - gestisce l'allineamento di tutti i client, mantenendoli in uno stato consistente
 *
 * @param players lista degli ActorRef dei giocatori in questa partita
 * @param mapUsername mappa che identifica il nome utente dal proprio ActorRef
 */
class GameServer(players : List[ActorRef], mapUsername : Map[ActorRef, String]) extends Actor {

  val mediator = DistributedPubSub(context.system).mediator
  val serverTopic = GAME_SERVER_SEND_TOPIC+self.toString().substring(self.toString().indexOf('#'))
  val chatTopic : String = CHAT_TOPIC+self.toString().substring(self.toString().indexOf('#'))
  mediator ! Subscribe(serverTopic, self)
  mediator ! Subscribe(chatTopic, self)
  private val cluster = Cluster.get(context.system)
  private val scheduler: CustomScheduler = ClusterScheduler(cluster)
  private val nPlayer = players.size

  private var greetingServerRef: ActorRef = _
  private val gamePlayers = players
  private val gamePlayersUsername: Map[ActorRef, String] = mapUsername
  private var winnerRef: ActorRef = _

  private val board = BoardImpl()
  private val pouch = LettersBagImpl()
  private var playersHand = Map[ActorRef, LettersHandImpl]()
  //creo le mani
  gamePlayers.foreach(p => playersHand+=(p -> LettersHandImpl.apply(Vector(pouch.takeRandomElementFromBagOfLetters(8).get : _*))))
  private var playedWord : List[BoardTile] = List[BoardTile]()
  private var numberOfPlayedTileInHand = 0
  //creo il dizionario
  private val dictionaryPath: String = "/dictionary/dictionary.txt"
  private val dictionary: DictionaryImpl = new DictionaryImpl(dictionaryPath)

  private val ranking : Ranking = new RankingImpl(players)

  private var turn = 0
  private var isGameEnded : Boolean = false
  private var isFirstWord : Boolean = true

  //variabili ack
  private var ackTopicReceived = CounterImpl(nPlayer)
  private var ackTurn = CounterImpl(nPlayer)
  private var ackEndTurn = CounterImpl(nPlayer)
  private var ackEndGame = CounterImpl(nPlayer)
  private var ackDisconnection = CounterImpl(nPlayer)

  /**Fornisce la definizione del comportamento del GameServer.
   *  Essendo un server questo deve poter rispondere ad ogni richiesta leggitima dei client durante la partita:
   *    - inizializzazione della partita
   *    - selezione del turno
   *    - gestione della giocata
   *    - aggiornamento di fine turno per mantenere la consistenza del tabellone
   *    - gestione della disconnessione di un giocatore e conseguente terminazione della partita
   *    - inoltoltro dei messaggi in chat
   */
  override def receive: Receive = {
    case _: InitGame =>
      scheduler.replaceBehaviourAndStart(() => sendTopic())
      greetingServerRef = sender()
    case _: MatchTopicListenAck =>
      ackTopicReceived.increment()
      if (ackTopicReceived.isFull) {
        scheduler.stopTask()
        ackTopicReceived.reset()
        scheduler.replaceBehaviourAndStart(() => sendTurn())
      }
    case _: PlayerTurnBeginAck =>
      ackTurn.increment()
      if (ackTurn.isFull) {
        scheduler.stopTask()
        ackTurn.reset()
      }
    //ricezione di una mossa
    case message: ClientMadeMove => message.move match {
      case _: Move.Pass =>
        if (sender().equals(gamePlayers(turn))) {
          sender ! ClientMoveAck(PassAck())
          scheduler.replaceBehaviourAndStart(() => sendUpdate())
        }
      case _: Move.TimeOut =>
        if (sender().equals(gamePlayers(turn))) {
          sender ! ClientMoveAck(TimeoutAck())
          scheduler.replaceBehaviourAndStart(() => sendUpdate())
        }
      case _: Move.Switch =>
        if (sender().equals(gamePlayers(turn))) {
          if (playersHand(sender()).containsOnlyVowelsOrOnlyConsonants()) {
            val nCard = playersHand(sender()).hand.size
            pouch.reinsertCardInBag(playersHand(sender()).hand)
            playersHand =  playersHand + (sender() -> LettersHandImpl.apply(Vector(pouch.takeRandomElementFromBagOfLetters(nCard).get: _*)))
            sender ! ClientMoveAck(HandSwitchRequestAccepted(playersHand(sender()).hand))
            scheduler.replaceBehaviourAndStart(() => sendUpdate())
          } else {
            sender ! ClientMoveAck(HandSwitchRequestRefused())
          }
        }
      case message: Move.WordMove =>
        if (sender().equals(gamePlayers(turn))) {
          playedWord = message.word
          board.addPlayedWord(List.concat(playedWord))
          if(isFirstWord && board.checkGameFirstWord() && dictionary.checkWords(board.getWordsFromLetters(board.takeCardToCalculatePoints(isFirstWord)))){
            updatePointsAndCheckIfGameEnded(sender())
            isFirstWord = false
          } else if (!isFirstWord && dictionary.checkWords(board.getWordsFromLetters(board.takeCardToCalculatePoints()))) {
            updatePointsAndCheckIfGameEnded(sender())
          } else {
            board.clearBoardFromPlayedWords()
            board.clearPlayedWords()
            sender ! ClientMoveAck(WordRefused())
          }
          playedWord = List[BoardTile]()
        }
    }

    case _: EndTurnUpdateAck =>
      ackEndTurn.increment()
      if (ackEndTurn.isFull) {
        scheduler.stopTask()
        ackEndTurn.reset()
        board.clearPlayedWords()
        if(!isGameEnded) {
          incrementTurn()
          scheduler.replaceBehaviourAndStart(() => sendTurn())
        } else {
          context.become(EndGame)
          self ! EndGameInit()
        }
      }

    //gestione disconnessione
    case _ : DisconnectionToGameServerNotification =>
      sender ! DisconnectionToGameServerNotificationAck()
      scheduler.stopTask()
      ackDisconnection.increment()
      if (ackDisconnection.isFull) {
        scheduler.stopTask()
        ackDisconnection.reset()
        scheduler.replaceBehaviourAndStart(()=>greetingServerRef ! EndGameToGreeting())
      } else {
        scheduler.replaceBehaviourAndStart(() => sendDisconnection())
      }

    case _: SomeoneDisconnectedAck =>
      ackDisconnection.increment()
      if (ackDisconnection.isFull) {
        scheduler.stopTask()
        ackDisconnection.reset()
        scheduler.replaceBehaviourAndStart(()=>greetingServerRef ! EndGameToGreeting())
      }

    case _ : EndGameToGreetingAck =>
      scheduler.stopTask()
      context.stop(self)

    //chat
    case message: SendChatMessageToGameServer =>
      gamePlayers.foreach(p => if (!p.equals(sender())) {
        p ! SendOnChat(message.senderUsername, sender(), message.message)
      })
  }

  /** Fornisce la definizione del comportamento del GameServer quando la partita termina con un vincitore;
   *  la procedura consiste in uno scambio di acknowledge per terminare la partita correttamente per ogni attore.
   */
  def EndGame : Receive = {
    case _ : EndGameInit =>
      scheduler.replaceBehaviourAndStart(() => mediator ! Publish(serverTopic,GameEnded(gamePlayersUsername(winnerRef), winnerRef)))
    case  _ : GameEndedAck =>
      ackEndGame.increment()
      if (ackEndGame.isFull) {
        ackEndGame.reset()
        scheduler.stopTask()
        scheduler.replaceBehaviourAndStart(()=>greetingServerRef ! EndGameToGreeting())
      }
    //se qualcuno si disconnette ora lo considero come un ack
    case _ : DisconnectionToGameServerNotification =>
      ackEndGame.increment()
      if (ackEndGame.isFull) {
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
    gamePlayers.foreach(player => player ! MatchTopicListenQuery(serverTopic, chatTopic, playersHand(player).hand, gamePlayersUsername.values.toList))
  }

  private def sendTurn(): Unit = {
    mediator ! Publish(serverTopic, PlayerTurnBegins(gamePlayers(turn)))
  }

  private def sendUpdate(): Unit = {
    val rankingTuples : List[(String,Int)] = gamePlayers.map(player => (gamePlayersUsername(player), ranking.ranking(player)))
    mediator ! Publish(serverTopic, EndTurnUpdate(rankingTuples, board.playedWord))
  }

  private def sendDisconnection(): Unit = {
    mediator ! Publish(serverTopic, SomeoneDisconnected())
  }

  //metodi utilità
  private def incrementTurn(){
    turn = (turn + 1) % nPlayer
  }
  private def replaceHand() : Unit = {
    for(boardTile <- playedWord){
      playersHand(sender()).playLetter(playersHand(sender()).hand.indexOf(CardImpl(boardTile.card.letter)))
      numberOfPlayedTileInHand = numberOfPlayedTileInHand + 1
    }
    val drawnTiles = pouch.takeRandomElementFromBagOfLetters(numberOfPlayedTileInHand).getOrElse(List())
    drawnTiles.indices.foreach(i => playersHand(sender()).putLetter(playersHand(sender()).hand.size, CardImpl(drawnTiles(i).letter)))
    numberOfPlayedTileInHand = 0
  }

  private def updatePointsAndCheckIfGameEnded(sender: ActorRef): Unit ={
    ranking.updatePoints(sender, board.calculateTurnPoints(board.takeCardToCalculatePoints(isFirstWord), isFirstWord))
    replaceHand()
    sender ! ClientMoveAck(WordAccepted(playersHand(sender).hand))
    if (playersHand(sender).hand.isEmpty && pouch.bag.isEmpty) {
      winnerRef = sender
      for (player <- gamePlayers) {
        ranking.removePoints(player, playersHand(player).calculateHandPoint)
        ranking.updatePoints(sender, playersHand(player).calculateHandPoint)
      }
      isGameEnded = true
    }
    scheduler.replaceBehaviourAndStart(() => sendUpdate())
  }
}