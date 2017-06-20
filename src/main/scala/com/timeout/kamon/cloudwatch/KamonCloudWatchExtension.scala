package com.timeout.kamon.cloudwatch

import akka.actor.{ActorRef, ExtendedActorSystem}
import akka.event.Logging
import com.timeout.kamon.cloudwatch.KamonSettings.subscriptions
import kamon.Kamon

import scala.concurrent.ExecutionContextExecutor


/**
  * Register pattern subscriptions with underlying subscriber actor
  */
class KamonCloudWatchExtension(system: ExtendedActorSystem) extends Kamon.Extension {
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  protected val log = Logging(system, classOf[KamonCloudWatchExtension])
  log.info("Starting the Kamon CloudWatch extension")

  val shipper: ActorRef = system.actorOf(MetricsShipper.props, "cloudwatch-metrics-shipper")
  val subscriber: ActorRef = system.actorOf(MetricsLogger.props(shipper), "cloudwatch-metrics-logger")

  // loop through patterns to register
  subscriptions.foreach { entry => entry._2.foreach { pattern =>
      log.info(s"""Subscribing to category "${entry._1}" pattern "$pattern"""")
      Kamon.metrics.subscribe(entry._1, pattern, subscriber, permanently = true)
    }
  }
}