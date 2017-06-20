package com.timeout.kamon.cloudwatch

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import com.amazonaws.services.cloudwatch.model._
import com.timeout.kamon.cloudwatch.KamonSettings.batchSize
import kamon.metric.SubscriptionsDispatcher.TickMetricSnapshot
import kamon.metric.instrument.{Counter, Histogram, Memory, Time}

import scala.collection.JavaConverters._

/**
  * Decides the logging of datums and the shipment to CloudWatch
  */
class MetricsLogger(shipper: ActorRef) extends Actor with ActorLogging {

  override def receive: Receive = LoggingReceive {
    case tick: TickMetricSnapshot =>
      data(tick).sliding(batchSize, batchSize).foreach { data =>
        // allow a latch to only log the metrics without pushing out onto Cloudwatch
        data.foreach(d => log.debug(d.toString))
        if (!KamonSettings.logOnly) shipper ! ShipMetrics(data)
      }

    case msg => log.warning(s"Unsupported message $msg received in MetricsLogger")
  }

  /**
    * Produce the datums.
    * Code is take from:
    * https://github.com/philwill-nap/Kamon/blob/master/kamon-cloudwatch/
    * src/main/scala/kamon/cloudwatch/CloudWatchMetricsSender.scala
    */
  private def data(tick: TickMetricSnapshot): List[MetricDatum] = {
    for {
      (groupIdentity, groupSnapshot) <- tick.metrics
      groupDimension = new Dimension().withName(groupIdentity.category).withValue(groupIdentity.name)
      groupDimensions = List(groupDimension).asJava
      (metricIdentity, metricSnapshot) <- groupSnapshot.metrics
    } yield {
      val (unit, correctionFactor) = metricIdentity.unitOfMeasurement match {
        case Time.Nanoseconds => StandardUnit.Microseconds -> 1E-3
        case Time.Microseconds => StandardUnit.Microseconds -> 1.0
        case Time.Milliseconds => StandardUnit.Milliseconds -> 1.0
        case Time.Seconds => StandardUnit.Seconds -> 1.0
        case Memory.Bytes => StandardUnit.Bytes -> 1.0
        case Memory.KiloBytes => StandardUnit.Kilobytes -> 1.0
        case Memory.MegaBytes => StandardUnit.Megabytes -> 1.0
        case Memory.GigaBytes => StandardUnit.Gigabytes -> 1.0
        case _ => StandardUnit.Count -> 1.0
      }

      val datum = new MetricDatum()
        .withDimensions(groupDimensions)
        .withMetricName(metricIdentity.name)
        .withTimestamp(new Date())

      metricSnapshot match {
        case hs: Histogram.Snapshot if hs.numberOfMeasurements > 0 =>
          val statSet = new StatisticSet()
            .withMaximum(hs.max.toDouble * correctionFactor)
            .withMinimum(hs.min.toDouble * correctionFactor)
            .withSampleCount(hs.numberOfMeasurements.toDouble)
            .withSum(hs.sum.toDouble * correctionFactor)
          List(datum.withStatisticValues(statSet).withUnit(unit))

        case cs: Counter.Snapshot =>
          List(datum.withValue(cs.count.toDouble * correctionFactor).withUnit(unit))

        case _ => List.empty
      }
    }
  }.toList.flatten
}

object MetricsLogger {
  def props(shipper: ActorRef): Props = Props(new MetricsLogger(shipper))
}