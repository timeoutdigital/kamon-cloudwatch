package com.timeout.kamon.cloudwatch

import com.typesafe.config.ConfigFactory


object KamonSettings {

  private[cloudwatch] val conf = ConfigFactory.load
  private[cloudwatch] val cloudWatchConfig = conf.getConfig("kamon.cloudwatch")
  private[cloudwatch] val subscriptions = cloudWatchConfig.getConfig("subscriptions")
  private[cloudwatch] val nameSpace = cloudWatchConfig.getString("name-space")
  private[cloudwatch] val region = cloudWatchConfig.getString("region")
  private[cloudwatch] val batchSize = cloudWatchConfig.getInt("batch-size")
  private[cloudwatch] val logOnly = cloudWatchConfig.getBoolean("log-metrics-only")
}