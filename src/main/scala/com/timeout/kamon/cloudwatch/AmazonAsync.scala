package com.timeout.kamon.cloudwatch

import java.util.concurrent
import java.util.concurrent.CancellationException

import com.amazonaws.{AmazonWebServiceRequest, AmazonWebServiceResult, ResponseMetadata}
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync
import com.amazonaws.services.cloudwatch.model.{MetricDatum, PutMetricDataRequest}

import scala.collection.JavaConverters._
import scala.concurrent.{Future, Promise}
import scala.util.Try

object AmazonAsync {

  type MetricDatumBatch = Vector[MetricDatum]

  def asyncRequest[Arg, Req <: AmazonWebServiceRequest, Res](asyncArg: Arg)
  (asyncOp: (Arg, AsyncHandler[Req, Res]) => concurrent.Future[Res]): Future[Res] = {

    val promise: Promise[Res] = Promise[Res]
    val handler = new AsyncHandler[Req, Res] {
      override def onError(exception: Exception): Unit =
        promise.failure(new CancellationException(s"AWS async command is cancelled."))
      override def onSuccess(request: Req, result: Res): Unit = promise.complete(Try(result))
    }
    asyncOp(asyncArg, handler)
    promise.future
  }

  implicit class MetricsAsyncOps(data: MetricDatumBatch) {

    def put(nameSpace: String)(implicit client: AmazonCloudWatchAsync): Future[AmazonWebServiceResult[ResponseMetadata]] =
      asyncRequest(new PutMetricDataRequest()
        .withNamespace(nameSpace)
        .withMetricData(data.asJava))(client.putMetricDataAsync)

  }
}
