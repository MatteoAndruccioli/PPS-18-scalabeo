package shared

import java.util.concurrent.TimeUnit
import akka.actor.Cancellable
import akka.cluster.Cluster
import scala.concurrent.duration.Duration


//implementa uno scheduler utilizzabile dagli attori del cluster specificato
//espone funzioni di utility che facilitano il rimpiazzo del task schedulato
class ClusterScheduler(var initialDelay: Long,
                       var timeUnitInitialDelay: TimeUnit,
                       var interval: Long,
                       var timeUnitInterval: TimeUnit,
                       var runnable: Option[Runnable],
                       cluster: Cluster) {
  val ec = cluster.system.dispatcher
  def initialDelayTime = Duration.create(initialDelay, timeUnitInitialDelay)
  def intervalTime = Duration.create(interval, timeUnitInterval)

  //questo Optional contiene il Cancellable task in esecuzione
  var cancellable :Option[Cancellable] = None
  //true quando il task sta eseguendo
  var running:Boolean = false

  //avvia lo scheduling dell'attività presente in cancellable
  def startTask():Unit = {
    runnable match {
      case Some(action) => {
        cancellable = Some(cluster.system.scheduler.schedule(initialDelayTime, intervalTime, action)(ec))
        running = true
      }
      case _ => println("runnable non riconosciuto")
    }
  }

  //permette di stoppare lo scheduler
  def stopTask(): Unit = {
    if(running){
      running = false
      stopScheduler
    }
  }

  //stoppa l'esecuzione del task attualmente in esecuzione
  private def stopScheduler:Unit = {
    cancellable match {
      case Some(task) => task.cancel()
      case _ => println("Errore stop scheduler")
    }
  }

  //modifichi solo il runnable
  def quickBehaviourReset(newTask: Runnable):Boolean = {
    if(running){
      false
    }else{
      runnable = Some(newTask)
      cancellable  = None
      true
    }
  }

  //stoppa task corrente, lo rimpiazza con quello indicato e riavvia lo scheduler
  def replaceBehaviourAndStart(newTask: Runnable): Unit ={
    stopTask() //aggiunto per sicurezza, se già stoppato non fa niente e passa alla prossima istruzione
    quickBehaviourReset(newTask)
    startTask()
  }
}