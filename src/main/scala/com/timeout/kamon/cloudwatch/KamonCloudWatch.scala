package com.timeout.kamon.cloudwatch

import akka.actor.{ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}


object KamonCloudWatch extends ExtensionId[KamonCloudWatchExtension] with ExtensionIdProvider {

  override def createExtension(system: ExtendedActorSystem): KamonCloudWatchExtension = new KamonCloudWatchExtension(system)

  override def lookup(): ExtensionId[_ <: Extension] = KamonCloudWatch
}
