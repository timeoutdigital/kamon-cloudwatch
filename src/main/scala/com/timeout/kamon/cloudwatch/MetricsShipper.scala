package com.timeout.kamon.cloudwatch

import java.util.concurrent.{ExecutorService, Executors}

import akka.pattern.pipe
import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingReceive
import com.amazonaws.client.builder.ExecutorFactory
import com.amazonaws.services.cloudwatch.model.{MetricDatum, PutMetricDataRequest, PutMetricDataResult}
import com.amazonaws.services.cloudwatch.{AmazonCloudWatchAsync, AmazonCloudWatchAsyncClientBuilder}
import com.timeout.kamon.cloudwatch.KamonSettings.{nameSpace, region}
import com.timeout.kamon.cloudwatch.FutureUtils._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext


/**
  * Ship-and-forget. Let the java future to process the actual shipment.
  */
class MetricsShipper(implicit ec: ExecutionContext) extends Actor with ActorLogging {

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
      val f = client.putMetricDataAsync(
        new PutMetricDataRequest().withNamespace(nameSpace).withMetricData(metrics.asJava)
      )
      f.asScala.pipeTo(self)

    case msg: PutMetricDataResult =>
      log.debug(s"Success: pushed metrics to Cloudwatch: $msg")

    case msg => log.warning(s"Unsupported message $msg received in MetricsShipper")
  }
}

object MetricsShipper {
  def props(implicit ec: ExecutionContext): Props = Props(new MetricsShipper)
}

final case class ShipMetrics(datums: List[MetricDatum])
