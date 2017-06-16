package com.timeout.kamon.cloudwatch

import akka.actor.{ActorRef, ExtendedActorSystem}
import akka.event.Logging
import com.timeout.kamon.cloudwatch.KamonSettings.subscriptions
import com.typesafe.config.ConfigList

import scala.collection.JavaConverters._
import kamon.Kamon


class KamonCloudWatchExtension(system: ExtendedActorSystem) extends Kamon.Extension {
  val log = Logging(system, classOf[KamonCloudWatchExtension])
  log.info("Starting the Kamon CloudWatch extension")

  val shipper: ActorRef = system.actorOf(MetricsShipper.props(), "cloudwatch-metrics-shipper")
  val subscriber: ActorRef = system.actorOf(MetricsLogger.props(shipper), "cloudwatch-metrics-logger")

  subscriptions.entrySet.asScala.foreach { entry =>
      val category = entry.getKey
      val patternList = entry.getValue.asInstanceOf[ConfigList]
      patternList.unwrapped.asScala.foreach { p =>
        val pattern = p.asInstanceOf[String] // the selection
        log.info(s"""Subscribing to category "$category" pattern "$pattern"""")
        Kamon.metrics.subscribe(category, pattern, subscriber, permanently = true)
      }
  }
}
