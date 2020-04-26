package shared

import java.util.concurrent.TimeUnit
import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.Cluster
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.must.Matchers
import scala.concurrent.duration.FiniteDuration

/** costanti usate nell'esecuzione dei test */
object Constants {
  val testMessage: String = "testMessage"
  val testMessage2: String = "testMessage2"
  val testMessage3: String = "testMessage3"
  val testMessage4: String = "testMessage4"
  val times: Int = 5
  val secondsWithoutMessages: Int = 5
}

/** è una classe di test per un oggetto di tipo CustomScheduler */
class CustomSchedulerTest extends TestKit(ActorSystem("test-system"))
  with AnyFlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers {


  //arresta ActorSystem dopo che tutti i test sono terminati
  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }


  /** questo oggetto si trova dentro alla classe perchè il metodo 'checkMessageReceived'
   *  necessita di 'Matchers' per funzionare perchè usa must
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

    /** permette di istanziare lo scheduler */
    def istantiateEmptyScheduler(cluster: Cluster, runnable: Option[Runnable]): CustomScheduler = ClusterScheduler(initialDelay, TimeUnit.SECONDS, interval, TimeUnit.SECONDS, runnable,cluster)

    /** Crea un task che invia un messaggio a un attore
     *
     *  @param receiver =>  ActorRef dell'attore che riceverà il messaggio
     *  @param msg      =>  messaggio che si vuole inviare
     *  @return task (Runnable) che invia msg al receiver
     */
    def createTask(receiver: ActorRef, msg: String): Runnable = ()=>{receiver ! msg}



    /** Esegue un task molteplici volte
     *
     *  @param times numero di volte per cui il task deve essere eseguito
     *  @param runnable task che deve essere eseguito
     */
    def runMultipleTimes(times:Int, runnable:Runnable) = for(i <- one to times) {println(i); runnable.run()}


    /** rappresenta un ciclo di esecuzione di un task attraverso lo scheduler:
     *    - avvia lo scheduler (il task eseguito dallo scheduler deve già esser stato impostato)
     *    - nel mentre che lo scheduler esegue, viene eseguito il Runnable 'task' per 'times' volte
     *    - stoppa lo scheduler
     * @param scheduler scheduler utilizzato
     * @param times numero di volte per cui il task deve essere eseguito
     * @param task runnable task che deve essere eseguito
     */
    def testMultipleTimesTask(scheduler:CustomScheduler, times:Int, task:Runnable):Unit = {
      scheduler.startTask()
      runMultipleTimes(times, task)
      scheduler.stopTask()
    }


    /** simile a testMultipleTimesTask
     *    ma usa il metodo replaceBehaviourAndStart per rimpiazzare il task precedente
     *
     *  non stoppa il task
     *
     * @param scheduler scheduler scheduler utilizzato
     * @param times numero di volte per cui il task deve essere eseguito
     * @param testerTask vecchio task che deve essere eseguito
     * @param newTask nuovo task che deve essere eseguito
     */
    def replaceMultipletestTask(scheduler:CustomScheduler, times:Int, testerTask:Runnable, newTask:Runnable):Unit = {
      scheduler.replaceBehaviourAndStart(newTask)
      runMultipleTimes(times, testerTask)
    }


    /** come replaceMultipletestTask, ma chiama stop sul task dopo aver eseguito tutti i test
     *
     * @param scheduler scheduler scheduler utilizzato
     * @param times numero di volte per cui il task deve essere eseguito
     * @param testerTask vecchio task che deve essere eseguito
     * @param newTask nuovo task che deve essere eseguito
     */
    def replaceMultipletestStopTask(scheduler:CustomScheduler, times:Int, testerTask:Runnable, newTask:Runnable):Unit = {
      replaceMultipletestTask(scheduler, times, testerTask, newTask)
      scheduler.stopTask()
    }


    /** crea un task che valuta se il messaggio ricevuto dal receiver:
     *    - è di tipo String
     *    - contiene la stringa indicata
     *
     * @param message messaggio che deve essere inviato
     * @param receiver attore ricevente
     * @return task realizzato
     */
    def checkMessageReceived(message: String, receiver: TestProbe): Runnable = ()=>{
      val state = receiver.expectMsgType[String]
      state must equal(message)
      println(message)
    }

  }

  /** test che:
   *    - istanzia lo scheduler con un determinato task: invio di un messaggio
   *    - testa che lo scheduler mandi quel messaggio 'times' volte:
   *        + chiama runMultipleTime indicando i controlli da eseguire sul messaggio ricevuto
   *    - controlla che dopo aver chiamato stopQuerying lo scheduler smetta di inviare messaggi
   *        + valuto questo controllando che non vengano ricevuti msg per un certo numero
   *           di secondi dopo stopQuerying()
   */
  "Scheduler" should "execute task multiple time" in {
    import Constants._
    import UtilityFunctions._

    val receiver = TestProbe()
    val scheduler: CustomScheduler = istantiateEmptyScheduler(Cluster.get(system), Some(createTask(receiver.ref, testMessage)))
    testMultipleTimesTask(scheduler, times, checkMessageReceived(testMessage,receiver))
    expectNoMessage(new FiniteDuration(secondsWithoutMessages,TimeUnit.SECONDS))
  }

  /** test sul metodo replaceBehaviourAndStart:
   *    - si tratta di verificare la possibilità di riassegnare il task allo scheduler
   *    - voglio verificare che il task assegnato precedentemente allo scheduler
   *        venga interrotto ed assegnato il nuovo task
   *    - verifica che non sia necessario sospendere il task precedente manualmente
   */
  "Scheduler.replaceBehaviourAndRun" should "let user change task running with a new one and run the latest" in {
    import Constants._
    import UtilityFunctions._

    val receiver = TestProbe()
    val scheduler: CustomScheduler = istantiateEmptyScheduler(Cluster.get(system), Some(createTask(receiver.ref, testMessage))) //creo scheduler con task 1 (invio ping msg)

    scheduler.startTask()
    runMultipleTimes(times, checkMessageReceived(testMessage,receiver))//controlli su ricezione ping msg
    //nota quà non stoppo il task corrente

    replaceMultipletestTask(scheduler, times, checkMessageReceived(testMessage2,receiver), createTask(receiver.ref, testMessage2))
    //nota quà non stoppo il task corrente
    replaceMultipletestStopTask(scheduler, times, checkMessageReceived(testMessage3,receiver), createTask(receiver.ref, testMessage3))
    //nota quà stoppo il task corrente => non dovrebbero più arrivare messaggi dopo lo stop
    expectNoMessage(new FiniteDuration(secondsWithoutMessages,TimeUnit.SECONDS))
  }


  /** test sul metodo replaceBehaviourAndStart:
   *    - verifico che partendo da uno scheduler creato con un Option(task) == None,
   *        posso sfruttando questo metodo assegnare un
   *        primo task e avviare lo scheduler
   */
  "Scheduler.replaceBehaviourAndRun" should "let user set & run a task for a scheduler " +
    "instantiated with a Option(task) == None " in {
    import Constants._
    import UtilityFunctions._

    val receiver = TestProbe()
    val scheduler: CustomScheduler = istantiateEmptyScheduler(Cluster.get(system), None) //creo scheduler con task 1 (invio ping msg)

    replaceMultipletestTask(scheduler, times, checkMessageReceived(testMessage,receiver), createTask(receiver.ref, testMessage))
    //nota quà non stoppo il task corrente
    replaceMultipletestTask(scheduler, times, checkMessageReceived(testMessage2,receiver), createTask(receiver.ref, testMessage2))
    //nota quà non stoppo il task corrente
    replaceMultipletestStopTask(scheduler, times, checkMessageReceived(testMessage3,receiver), createTask(receiver.ref, testMessage3))
    //nota quà stoppo il task corrente => non dovrebbero più arrivare messaggi dopo lo stop
    expectNoMessage(new FiniteDuration(secondsWithoutMessages,TimeUnit.SECONDS))
  }


  /** test sul metodo replaceBehaviourAndStart:
   *    - verifico che partendo da uno scheduler su cui è già stato chiamato stopQuerying()
   *      la chiamata a  replaceBehaviourAndRun(newRunnable: Runnable) funzioni correttamente:
   *      ovvero consente assegnazione di nuovo task e sua esecuzione
   */
  "Scheduler.replaceBehaviourAndRun" should "let user change stopped task with a new one and run the latest" in {
    import Constants._
    import UtilityFunctions._

    val receiver = TestProbe()
    val scheduler: CustomScheduler = istantiateEmptyScheduler(Cluster.get(system), None) //creo scheduler con task 1 (invio ping msg)

    replaceMultipletestStopTask(scheduler, times, checkMessageReceived(testMessage,receiver), createTask(receiver.ref, testMessage))
    //nota quà stoppo il task corrente
    replaceMultipletestTask(scheduler, times, checkMessageReceived(testMessage2,receiver), createTask(receiver.ref, testMessage2))
    //nota quà non stoppo il task corrente
    replaceMultipletestStopTask(scheduler, times, checkMessageReceived(testMessage3,receiver), createTask(receiver.ref, testMessage3))
    //nota quà stoppo il task corrente => non dovrebbero più arrivare messaggi dopo lo stop
    expectNoMessage(new FiniteDuration(secondsWithoutMessages,TimeUnit.SECONDS))
  }

}
