package com.timeout.kamon.cloudwatch

import com.typesafe.config.{ConfigFactory, ConfigList}

import scala.collection.JavaConverters._

object KamonSettings {

  private[cloudwatch] val conf = ConfigFactory.load
  private[cloudwatch] val cloudWatchConfig = conf.getConfig("kamon.cloudwatch")
  private[cloudwatch] val subscriptions =
    cloudWatchConfig.getConfig("subscriptions").entrySet.asScala.map { entry =>
      entry.getKey -> entry.getValue.asInstanceOf[ConfigList].unwrapped.asScala.map(_.asInstanceOf[String])
    }

  private[cloudwatch] val nameSpace = cloudWatchConfig.getString("name-space")
  private[cloudwatch] val region = cloudWatchConfig.getString("region")
  private[cloudwatch] val batchSize = cloudWatchConfig.getInt("batch-size")
  private[cloudwatch] val logOnly = cloudWatchConfig.getBoolean("log-metrics-only")
  private[cloudwatch] val numThreads = cloudWatchConfig.getInt("async-threads")
}