package shared

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.Cluster
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.must.Matchers

import scala.concurrent.duration.FiniteDuration


object Constants {
  val testMessage: String = "testMessage"
  val times: Int = 5
  val secondsWithoutMessages: Int = 5
}

//è una classe di test per un oggetto di tipo CustomScheduler
class CustomSchedulerTest extends TestKit(ActorSystem("test-system"))
  with AnyFlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers {


  //arresta ActorSystem dopo che tutti i test sono terminati
  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }



  /* questo oggetto si trova dentro alla classe perchè il metodo 'checkMessageReceived'
   necessita di 'Matchers' per funzionare perchè usa must
 */
  object UtilityFunctions {
    val one: Int = 1
    /*
      questo valore deve rimanere basso (3 è già troppo) perchè 'receiver.expectMsgType[String]' ha un timer interno di default molto basso
      si potrebbe usare una funzione simile a quella ma che permetta di modificare il timeout, tuttavia questi valori non sono importanti
      nel contesto del programma quindi tanto vale testarlo con valori bassi per una questione di semplicità e leggibilità
     */
    val initialDelay: Long = 1
    val interval: Long = 1

    //permette di istanziare lo scheduler
    def istantiateEmptyScheduler(cluster: Cluster, runnable: Option[Runnable]): CustomScheduler = ClusterScheduler(initialDelay, TimeUnit.SECONDS, interval, TimeUnit.SECONDS, runnable,cluster)

    /*
       Input:
        - receiver  =>  ActorRef dell'attore che riceverà il messaggio
        - msg       =>  messaggio che di vuole inviare

       restituisce un task (Runnable) che invia msg al receiver
     */
    def createTask(receiver: ActorRef, msg: String): Runnable = ()=>{receiver ! msg}


    //Esegue il task(Runnable) per un numero di volte indicato in 'times'
    def runMultipleTimes(times:Int, runnable:Runnable) = for(i <- one to times) {println(i); runnable.run()}

    /*
      rappresenta un ciclo di esecuzione di un task attraverso lo scheduler:
      - avvia lo scheduler (il task eseguito dallo scheduler deve già esser stato impostato)
      - nel mentre che lo scheduler esegue, viene eseguito il Runnable 'task' per 'times' volte
      - stoppa lo scheduler
   */
    def testMultipleTimesTask(scheduler:CustomScheduler, times:Int, task:Runnable):Unit = {
      scheduler.startTask()
      runMultipleTimes(times, task)
      scheduler.stopTask()
    }

    /*
      restituisce un task che valuta se il messaggio ricevuto dal receiver:
      - è di tipo String
      - contiene la stringa indicata
    */
      def checkMessageReceived(message: String, receiver: TestProbe): Runnable = ()=>{
        val state = receiver.expectMsgType[String]
        state must equal(message)
        println(message)
      }

  }




  /*
questo test:
  - istanzia lo scheduler con un determinato task: invio di un messaggio
  - testa che lo scheduler mandi quel messaggio 'times' volte:
     + chiama runMultipleTime indicando i controlli da eseguire sul messaggio ricevuto
  - controlla che dopo aver chiamato stopQuerying lo scheduler smetta di inviare messaggi
      + valuto questo controllando che non vengano ricevuti msg per un certo numero
        di secondi dopo stopQuerying()
*/
  "Scheduler" should "execute task multiple time" in {
    import Constants._
    import UtilityFunctions._

    val receiver = TestProbe()
    val scheduler: CustomScheduler = istantiateEmptyScheduler(Cluster.get(system), Some(createTask(receiver.ref, testMessage)))
    testMultipleTimesTask(scheduler, times, checkMessageReceived(testMessage,receiver))
    expectNoMessage(new FiniteDuration(secondsWithoutMessages,TimeUnit.SECONDS))
  }


}
