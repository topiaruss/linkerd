package io.buoyant.linkerd.protocol.http

import com.twitter.finagle.http.{Method, Request, Response, Status}
import com.twitter.finagle.service.{ResponseClass, ReqRep, ResponseClassifier}
import com.twitter.util.{Return, Throw, Try}
import org.scalatest.FunSuite

class ResponseClassifiersTest extends FunSuite {

  def testClassifier(
    classifier: ResponseClassifier,
    method: Method,
    status: Try[Status],
    classification: Option[ResponseClass]
  ): Unit = {
    val key = ReqRep(Request(method, "/"), status.map(Response(_)))
    classification match {
      case None =>
        assert(!classifier.isDefinedAt(key))
      case Some(classification) =>
        assert(classifier.isDefinedAt(key))
        assert(classifier(key) == classification)
    }
  }

  for {
    (classifier, methods) <- Map(
      ResponseClassifiers.Idempotent ->
        Seq(Method.Get, Method.Head, Method.Put, Method.Delete, Method.Options, Method.Trace),
      ResponseClassifiers.ReadOnly ->
        Seq(Method.Get, Method.Head, Method.Options, Method.Trace)
    )
    method <- methods
  } {

    test(s"$classifier: $method 500") {
      testClassifier(
        classifier,
        method,
        Return(Status.InternalServerError),
        Some(ResponseClass.RetryableFailure)
      )
    }

    test(s"$classifier: $method error") {
      testClassifier(
        classifier,
        method,
        Throw(new Exception),
        Some(ResponseClass.RetryableFailure)
      )
    }

    test(s"$classifier: ${method} 200") {
      testClassifier(classifier, method, Return(Status.Ok), Some(ResponseClass.Success))
    }

    test(s"$classifier: ${method} 404") {
      testClassifier(classifier, method, Return(Status.NotFound), Some(ResponseClass.Success))
    }
  }
}
