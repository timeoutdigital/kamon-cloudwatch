package com.timeout.kamon.cloudwatch

import java.util.concurrent.CancellationException

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync
import com.amazonaws.services.cloudwatch.model.{MetricDatum, PutMetricDataRequest, PutMetricDataResult}
import com.timeout.kamon.cloudwatch.KamonSettings.nameSpace

import scala.collection.JavaConverters._
import scala.concurrent.{Future, Promise}
import scala.util.Try

object MetricsAsyncOps {

    def putMetricDataAsync(data: List[MetricDatum])(implicit client: AmazonCloudWatchAsync): Future[PutMetricDataResult] = {
      val promise: Promise[PutMetricDataResult] = Promise[PutMetricDataResult]()
      client.putMetricDataAsync(
        new PutMetricDataRequest().withNamespace(nameSpace).withMetricData(data.asJava),
        asyncHandler[PutMetricDataRequest, PutMetricDataResult](promise)
      )
      promise.future
    }

    private def asyncHandler[Req <: AmazonWebServiceRequest, Result](promise: Promise[Result]) = {
      new AsyncHandler[Req, Result] {
        override def onError(exception: Exception): Unit =
          promise.failure(new CancellationException(s"AWS async command is cancelled."))
        override def onSuccess(request: Req, result: Result): Unit = promise.complete(Try(result))
      }
    }
}