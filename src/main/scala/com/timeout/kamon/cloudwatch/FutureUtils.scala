package com.timeout.kamon.cloudwatch


import java.util.concurrent.{CancellationException, Future => JavaFuture}

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try


object FutureUtils {

  implicit class FutureOps[T](jFuture: JavaFuture[T]) {
    val promise: Promise[T] = Promise[T]()

    /**
      * Recursive polling may be bad, should use timed polling, or jus use the blocking one below
      */
    def asScala: Future[T] = {
      @tailrec
      def checkCompletion(): Any = {
        if (jFuture.isCancelled) {
          promise.failure(new CancellationException(s"Java Future is cancelled. $jFuture"))
        } else if (jFuture.isDone) {
          promise.complete(Try(jFuture.get))
        } else checkCompletion()
      }

      checkCompletion()
      promise.future
    }

    def asScalaBlocking(implicit ec: ExecutionContext): Future[T] = Future {
      scala.concurrent.blocking {
        jFuture.get()
      }
    }
  }
}