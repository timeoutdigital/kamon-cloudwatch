package com.timeout.kamon.cloudwatch

import java.util.concurrent.{Callable, Executors}

import org.scalatest.{FreeSpec, Matchers}
import com.timeout.kamon.cloudwatch.FutureUtils.FutureOps

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong
import java.util.Random


class FutureUtilTest extends FreeSpec with Matchers {

  "A Java future" - {
    "should be turned into a Scala future" in {
      val threadPool = Executors.newSingleThreadExecutor
      val future = threadPool.submit(new Callable[Integer]() {
        @throws[Exception]
        override def call: Integer = 1
      })

      try {
        val scalaResult: Integer = Await.result(future.asScala, 3.seconds)
        val javaResult: Integer = future.get
        scalaResult equals javaResult
      } catch {
        case _: Throwable => fail()
      }
    }
  }
}
