package com.timeout.kamon.cloudwatch

import java.util.concurrent.{ExecutorService, Executors}

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingReceive
import com.amazonaws.client.builder.ExecutorFactory
import com.amazonaws.services.cloudwatch.model.{MetricDatum, PutMetricDataRequest}
import com.amazonaws.services.cloudwatch.{AmazonCloudWatchAsync, AmazonCloudWatchAsyncClientBuilder}
import com.timeout.kamon.cloudwatch.KamonSettings.{nameSpace, region}

import scala.collection.JavaConverters._


/**
  * Ship-and-forget. Let the java future to process the actual shipment.
  */
class MetricsShipper extends Actor with ActorLogging {

  // async aws client uses a thread pool that reuses a fixed number of threads
  // operating off a shared unbounded queue.
  val client: AmazonCloudWatchAsync = AmazonCloudWatchAsyncClientBuilder
    .standard()
    .withRegion(region)
    .withExecutorFactory(
      new ExecutorFactory {
        // don't use the default thread pool which configures 50 number of threads
        override def newExecutor(): ExecutorService = Executors.newFixedThreadPool(2)
      }
    ).build()


  override def receive: Receive = LoggingReceive {
    case ShipMetrics(metrics) =>
      client.putMetricDataAsync(
        new PutMetricDataRequest().withNamespace(nameSpace).withMetricData(metrics.asJava)
      )

    case msg => log.warning(s"Unsupported message $msg received in MetricsShipper")
  }
}

object MetricsShipper {
  def props(): Props = Props(new MetricsShipper)
}

final case class ShipMetrics(datums: List[MetricDatum])
