package com.timeout.kamon.cloudwatch

import java.time.{Clock, Instant}
import java.util.Date
import java.util.concurrent.atomic.AtomicReference

import com.typesafe.config.Config
import com.amazonaws.services.cloudwatch.model._
import com.timeout.kamon.cloudwatch.AmazonAsync.MetricDatumBatch

import kamon.{Kamon, MetricReporter, Tags}
import kamon.metric.{MeasurementUnit, MetricDistribution, MetricValue, PeriodSnapshot}

import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

case class Configuration(
  nameSpace: String,
  region: Option[String],
  batchSize: Int,
  sendMetrics: Boolean,
  numThreads: Int
)

class CloudWatchReporter private (clock: Clock) extends MetricReporter {

  def this() = this(Clock.systemUTC())

  private val logger = LoggerFactory.getLogger(classOf[CloudWatchReporter])

  private val configuration: AtomicReference[Configuration] =
    new AtomicReference()

  private val shipper: MetricsShipper = new MetricsShipper()

  override def start(): Unit = {
    logger.info("Starting the Kamon CloudWatch reporter")
    configuration.set(readConfiguration(Kamon.config()))
    shipper.reconfigure(configuration.get)
  }

  override def stop(): Unit = {}

  override def reconfigure(config: Config): Unit = {
    val current = configuration.get
    if (configuration.compareAndSet(current, readConfiguration(config))) {
      shipper.reconfigure(configuration.get)
      logger.info("Configuration reloaded successfully.")
    } else {
      logger.debug("Configuration hasn't changed from the last reload")
    }
  }

  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit = {
    val config = configuration.get

    if (config.sendMetrics) {
      val batch = datums(snapshot)
      shipper.shipMetrics(config.nameSpace, batch)
    }
  }

  private def readConfiguration(config: Config): Configuration = {
    val cloudWatchConfig = config.getConfig("kamon.cloudwatch")
    val nameSpace = cloudWatchConfig.getString("namespace")

    val region = {
      val regionName = {
        if (config.hasPath("region")) Option(cloudWatchConfig.getString("region"))
        else None
      }
      regionName.filterNot(_.isEmpty)
    }

    val batchSize = cloudWatchConfig.getInt("batch-size")
    val sendMetrics = cloudWatchConfig.getBoolean("send-metrics")
    val numThreads = cloudWatchConfig.getInt("async-threads")

    Configuration(nameSpace, region, batchSize, sendMetrics, numThreads)
  }

  /**
    * Produce the datums.
    * Code is take from:
    * https://github.com/philwill-nap/Kamon/blob/master/kamon-cloudwatch/
    * src/main/scala/kamon/cloudwatch/CloudWatchMetricsSender.scala
    */
  private def datums(snapshot: PeriodSnapshot): MetricDatumBatch = {
    def unitAndScale(unit: MeasurementUnit): (StandardUnit, Double) = {
      import MeasurementUnit.Dimension._
      import MeasurementUnit.{information, time}

      unit.dimension match {
        case Time if unit.magnitude == time.seconds.magnitude =>
          StandardUnit.Seconds -> 1.0
        case Time if unit.magnitude == time.milliseconds.magnitude =>
          StandardUnit.Milliseconds -> 1.0
        case Time if unit.magnitude == time.microseconds.magnitude =>
          StandardUnit.Microseconds -> 1.0
        case Time if unit.magnitude == time.nanoseconds.magnitude =>
          StandardUnit.Microseconds -> 1E-3

        case Information if unit.magnitude == information.bytes.magnitude =>
          StandardUnit.Bytes -> 1.0
        case Information if unit.magnitude == information.kilobytes.magnitude =>
          StandardUnit.Kilobytes -> 1.0
        case Information if unit.magnitude == information.megabytes.magnitude =>
          StandardUnit.Megabytes -> 1.0
        case Information if unit.magnitude == information.gigabytes.magnitude =>
          StandardUnit.Gigabytes -> 1.0

        case _ => StandardUnit.Count -> 1.0
      }
    }

    def datum(name: String, tags: Tags, unit: StandardUnit): MetricDatum = {
      val dimensions: List[Dimension] =
        tags.map {
          case (tagName, tagValue) => new Dimension().withName(tagName).withValue(tagValue)
        }.toList

      new MetricDatum()
        .withDimensions(dimensions.asJava)
        .withMetricName(name)
        .withTimestamp(Date.from(Instant.now(clock)))
        .withUnit(unit)
    }

    def datumFromDistribution(metric: MetricDistribution): MetricDatum = {
      val (unit, scale) = unitAndScale(metric.unit)
      val statSet = new StatisticSet()
        .withMaximum(metric.distribution.max.toDouble * scale)
        .withMinimum(metric.distribution.min.toDouble * scale)
        .withSampleCount(metric.distribution.count.toDouble)
        .withSum(metric.distribution.sum.toDouble * scale)

      datum(metric.name, metric.tags, unit)
        .withStatisticValues(statSet)
    }

    def datumFromValue(metric: MetricValue): MetricDatum = {
      val (unit, scale) = unitAndScale(metric.unit)
      datum(metric.name, metric.tags, unit)
        .withValue(metric.value.toDouble * scale)
    }

    val allDatums =
      snapshot.metrics.histograms.view.map(datumFromDistribution) ++
      snapshot.metrics.rangeSamplers.map(datumFromDistribution) ++
      snapshot.metrics.gauges.view.map(datumFromValue) ++
      snapshot.metrics.counters.view.map(datumFromValue)

    allDatums.toVector
  }
}
