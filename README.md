**Kamon Amazon Cloudwatch Extension**

[![Build Status](https://travis-ci.org/timeoutdigital/kamon-cloudwatch.svg?branch=master)](https://travis-ci.org/timeoutdigital/kamon-cloudwatch)
[![GitHub release](https://img.shields.io/github/tag/timeoutdigital/kamon-cloudwatch.svg)](https://github.com/timeoutdigital/kamon-cloudwatch/releases)


(Note: inspired by this repo done by [Phil Will](https://github.com/philwill-nap/Kamon/blob/master/kamon-cloudwatch). Some code has been reused and modified in this project)

# Overview
A simple [Kamon](https://github.com/kamon-io/Kamon) extension to ship metrics data to Cloudwatch using Amazon's async client.

# Installation
- add library dependency to your build.sbt
```scala
libraryDependencies += "com.timeout" %% "kamon-cloudwatch" % "0.0.1"
```

- load extension by Akka (optional), by default this module is set to auto-start by calling Kamon.start()
```scala
akka {
  ...
  
  extensions = ["com.timeout.kamon.cloudwatch.KamonCloudWatch"]
}
```

- make sure you have AWS_PROFILE or AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY pair set correctly

- add the following to your application.conf and change the fields accordingly:
```
kamon {
  cloudwatch {

    # namespace is the AWS Metrics custom namespace
    namespace = kamon-cloudwatch
    
    # AWS region, on ec2 region is fetched by getCurrentRegion command
    region = eu-west-1

    # batch size of data when send to Cloudwatch    
    batch-size = 20

    # only logs metrics to file without shipping out to Cloudwatch
    log-metrics-only = false

    # how many threads will be assigned to the pool that does the shipment of metrics
    async-threads = 5

    # Subscription patterns used to select which metrics will be pushed to CloudWatch.
    # Please Note that, metrics collection entries entities must be activated under the kamon.metrics.filters settings.
    # Paste the subscriptions template to your app to configure。
    subscriptions {
      histogram         = [ "**" ]
      min-max-counter   = [ "**" ]
      gauge             = [ "**" ]
      counter           = [ "**" ]
      trace             = [ "**" ]
      trace-segment     = [ "**" ]
      akka-actor        = [ 
        "project/user/userController/**",
        "project/user/orderController/**"
      ]
      akka-dispatcher   = [ "**" ]
      akka-router       = [ "**" ]
      system-metric     = [ "**" ]
      http-server       = [ "**" ]
      akka-http-server  = [ "**" ]
    }
  }
}
```

- module is set to auto-start by default, you should see "Starting the Kamon CloudWatch extension" in your console output.

# AWS Cloudwatch Example
- log on to Cloudwatch, the metrics will be appearing on 'Custom namespaces' section under "Metrics" menu, i.e.:
![alt text](https://github.com/timeoutdigital/kamon-cloudwatch/blob/master/doc/cloundwatch-metrics.png "what has showed up in Cloudwatch")