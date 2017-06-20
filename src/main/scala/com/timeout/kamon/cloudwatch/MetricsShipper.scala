package com.timeout.kamon.cloudwatch

import java.util.concurrent.{ExecutorService, Executors}

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingReceive
import akka.pattern.pipe
import com.amazonaws.client.builder.ExecutorFactory
import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudwatch.model.{MetricDatum, PutMetricDataResult}
import com.amazonaws.services.cloudwatch.{AmazonCloudWatchAsync, AmazonCloudWatchAsyncClientBuilder}
import com.timeout.kamon.cloudwatch.KamonSettings.region
import com.timeout.kamon.cloudwatch.MetricsAsyncOps.putMetricDataAsync

import scala.concurrent.ExecutionContext


/**
  * Ship-and-forget. Let the future to process the actual shipment to Cloudwatch.
  */
class MetricsShipper(implicit ec: ExecutionContext) extends Actor with ActorLogging {

  // async aws client uses a thread pool that reuses a fixed number of threads
  // operating off a shared unbounded queue.
  implicit val client: AmazonCloudWatchAsync = AmazonCloudWatchAsyncClientBuilder
    .standard()
    .withRegion(
      Option(Regions.getCurrentRegion).map(r => Regions.fromName(r.getName))
      .getOrElse(Regions.fromName(region))
    )
    .withExecutorFactory(
      new ExecutorFactory {
        // don't use the default thread pool which configures 50 number of threads
        override def newExecutor(): ExecutorService = Executors.newFixedThreadPool(KamonSettings.numThreads)
      }
    ).build()

  override def receive: Receive = LoggingReceive {
    case ShipMetrics(metrics) => putMetricDataAsync(metrics).pipeTo(self)
    case msg: PutMetricDataResult => log.debug(s"Success: pushed metrics to Cloudwatch: $msg")
    case Failure(t) => log.warning(s"Failed to send metrics to Cloudwatch ${t.getMessage}")
    case msg => log.warning(s"Unsupported message $msg received in MetricsShipper")
  }
}

object MetricsShipper {
  def props(implicit ec: ExecutionContext): Props = Props(new MetricsShipper)
}

final case class ShipMetrics(datums: List[MetricDatum])
