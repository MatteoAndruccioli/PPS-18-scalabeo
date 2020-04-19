package client

import akka.actor.{Actor, ActorRef}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import shared.ClientToGreetingMessages.ConnectionToGreetingQuery
import shared.Topic.GREETING_SERVER_RECEIVES_TOPIC

/*
  - è un attore che ascolta i messaggi inviati su Channels.GREETING_SERVER_RECEIVES_TOPIC
      e li rigira all'attore identificato da ActorRef dummyGreetingServer
  - avrei voluto usare un TestProbe ma non riesco a far si che tale TestProbe utilizzi il mediator
 */
class GreetingServerTopicListener(dummyGreetingServer: ActorRef) extends Actor{
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(GREETING_SERVER_RECEIVES_TOPIC, self)
  override def receive: Receive = {
    case connectionMessage : ConnectionToGreetingQuery => dummyGreetingServer ! connectionMessage
  }
}