package com.timeout.kamon.cloudwatch

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudwatch.{AmazonCloudWatchAsync, AmazonCloudWatchAsyncClientBuilder}
import com.timeout.kamon.cloudwatch.AmazonAsync.{MetricDatumBatch, MetricsAsyncOps}

import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * Ship-and-forget. Let the future to process the actual shipment to Cloudwatch.
  */
class MetricsShipper {
  private val logger = LoggerFactory.getLogger(classOf[MetricsShipper])

  // Kamon 1.0 requires to support hot-reconfiguration, which forces us to use an
  // AtomicReference here and hope for the best
  private val client: AtomicReference[AmazonCloudWatchAsync] = new AtomicReference()

  def reconfigure(configuration: Configuration): Unit = {
    def chosenRegion: Option[Regions] = {
      configuration.region
        .flatMap(r => Try(Regions.fromName(r)).toOption)
        .orElse(Option(Regions.getCurrentRegion).map(r => Regions.fromName(r.getName)))
    }

    // async aws client uses a thread pool that reuses a fixed number of threads
    // operating off a shared unbounded queue.
    def clientFromConfig: AmazonCloudWatchAsync = {
      val baseBuilder = AmazonCloudWatchAsyncClientBuilder.standard()
          .withExecutorFactory(() => Executors.newFixedThreadPool(configuration.numThreads))

      chosenRegion.fold(baseBuilder)(baseBuilder.withRegion).build()
    }

    client.set(clientFromConfig)
  }

  def shipMetrics(nameSpace: String, datums: MetricDatumBatch)(implicit ec: ExecutionContext): Future[Unit] = {
    implicit val currentClient: AmazonCloudWatchAsync = client.get
    datums.put(nameSpace)
      .map(result => logger.debug(s"Succeeded to push metrics to Cloudwatch: $result"))
      .recover {
        case error: Exception =>
          logger.warn(s"Failed to send metrics to Cloudwatch ${error.getMessage}")
      }
  }

}
