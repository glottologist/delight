package co.datamechanics.delight

import java.util.concurrent.atomic.AtomicBoolean

import org.apache.spark.internal.Logging
import org.apache.spark.scheduler._
import org.apache.spark.{JsonProtocolProxy, SparkConf}
import org.json4s.jackson.JsonMethods.{compact, render}


class DelightListener(sparkConf: SparkConf) extends SparkListener with Logging {

  private val heartbeatInterval = Configs.heartbeatInterval(sparkConf)

  private val streamingConnector = DelightStreamingConnector.getOrCreate(sparkConf)

  /**
    * Has the heartbeat thread started
    */
  private val started: AtomicBoolean = new AtomicBoolean(false)

  private def logEvent(event: SparkListenerEvent, flush: Boolean = false, blocking: Boolean = false): Unit = {
    try {
      val eventAsString = compact(render(JsonProtocolProxy.jsonProtocol.sparkEventToJson(event)))
      streamingConnector.enqueueEvent(eventAsString, flush = flush, blocking = blocking)
    } catch {
      case e: Exception =>
        logError(s"Failed to log event: ${e.getMessage}", e)
    }
    startIfNecessary()
  }

  /*
     The events that trigger a flush are the same as in org.apache.spark.scheduler.EventLoggingListener
     (the delight that creates Spark event logs).
     The only difference is that onBlockUpdated does not flush, to avoid flooding.
   */

  // Events that do not trigger a flush
  override def onStageSubmitted(event: SparkListenerStageSubmitted): Unit = logEvent(event)

  override def onTaskStart(event: SparkListenerTaskStart): Unit = logEvent(event)

  override def onTaskGettingResult(event: SparkListenerTaskGettingResult): Unit = logEvent(event)

  override def onTaskEnd(event: SparkListenerTaskEnd): Unit = logEvent(event)

  override def onEnvironmentUpdate(event: SparkListenerEnvironmentUpdate): Unit = {
    logEvent(event)
  }

  override def onBlockUpdated(event: SparkListenerBlockUpdated): Unit = {
    logEvent(event)
  }

  // Events that trigger a flush
  override def onStageCompleted(event: SparkListenerStageCompleted): Unit = {
    logEvent(event, flush = true)
  }

  override def onJobStart(event: SparkListenerJobStart): Unit = logEvent(event, flush = true)

  override def onJobEnd(event: SparkListenerJobEnd): Unit = logEvent(event, flush = true)

  override def onBlockManagerAdded(event: SparkListenerBlockManagerAdded): Unit = {
    logEvent(event, flush = true)
  }

  override def onBlockManagerRemoved(event: SparkListenerBlockManagerRemoved): Unit = {
    logEvent(event, flush = true)
  }

  override def onUnpersistRDD(event: SparkListenerUnpersistRDD): Unit = {
    logEvent(event, flush = true)
  }

  override def onApplicationStart(event: SparkListenerApplicationStart): Unit = {
    logEvent(event, flush = true)
  }

  override def onApplicationEnd(event: SparkListenerApplicationEnd): Unit = {
    // Upon ApplicationEnd, this thread waits for all pending messages to be sent to the server (`blocking`).
    // Otherwise Spark would exit before all pending messages are sent to the server.
    logEvent(event, flush = true, blocking = true)
  }
  override def onExecutorAdded(event: SparkListenerExecutorAdded): Unit = {
    logEvent(event, flush = true)
  }

  override def onExecutorRemoved(event: SparkListenerExecutorRemoved): Unit = {
    logEvent(event, flush = true)
  }

  override def onExecutorBlacklisted(event: SparkListenerExecutorBlacklisted): Unit = {
    logEvent(event, flush = true)
  }

  override def onExecutorBlacklistedForStage(event: SparkListenerExecutorBlacklistedForStage): Unit = {
    logEvent(event, flush = true)
  }

  override def onNodeBlacklistedForStage(event: SparkListenerNodeBlacklistedForStage): Unit = {
    logEvent(event, flush = true)
  }

  override def onExecutorUnblacklisted(event: SparkListenerExecutorUnblacklisted): Unit = {
    logEvent(event, flush = true)
  }

  override def onNodeBlacklisted(event: SparkListenerNodeBlacklisted): Unit = {
    logEvent(event, flush = true)
  }

  override def onNodeUnblacklisted(event: SparkListenerNodeUnblacklisted): Unit = {
    logEvent(event, flush = true)
  }

  // No-op because logging every update would be overkill
  override def onExecutorMetricsUpdate(event: SparkListenerExecutorMetricsUpdate): Unit = { }

  override def onOtherEvent(event: SparkListenerEvent): Unit = {
    logEvent(event, flush = true)
  }

  /**
    * Start the heartbeat thread that `sendHeartbeat()` every `heartbeatInterval` seconds
    *
    * - The thread is started only is it has not started yet.
    */
  private def startIfNecessary(): Unit = {
    if(started.compareAndSet(false, true)) {
      logInfo("Started DelightListener heartbeat thread")
      val thread = new Thread {
        override def run() {
          while (true) {
            logDebug("Logged heartbeat")
            streamingConnector.sendHeartbeat()
            Thread.sleep(heartbeatInterval.toMillis)
          }
        }
      }
      thread.start()
    }
  }

}

