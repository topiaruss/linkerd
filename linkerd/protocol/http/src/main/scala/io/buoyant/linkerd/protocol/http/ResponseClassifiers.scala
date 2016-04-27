package io.buoyant.linkerd.protocol.http

import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finagle.service.{ResponseClass, ReqRep, ResponseClassifier}
import com.twitter.util.{NonFatal, Return, Throw}

object ResponseClassifiers {

  object Requests {
    /** Matches read-only requests */
    object ReadOnly {
      def unapply(req: Request): Boolean = req.method match {
        case Method.Get
          | Method.Head
          | Method.Options
          | Method.Trace => true
        case _ => false
      }
    }

    /**
     * Matches idempotent requests.
     *
     * Per RFC2616:
     *
     *   Methods can also have the property of "idempotence" in that
     *   (aside from error or expiration issues) the side-effects of N >
     *   0 identical requests is the same as for a single request. The
     *   methods GET, HEAD, PUT and DELETE share this property. Also,
     *   the methods OPTIONS and TRACE SHOULD NOT have side effects, and
     *   so are inherently idempotent.
     */
    object Idempotent {
      def unapply(req: Request): Boolean = req.method match {
        case Method.Get
          | Method.Head
          | Method.Put
          | Method.Delete
          | Method.Options
          | Method.Trace => true
        case _ => false
      }
    }
  }

  object Responses {
    object Failure {
      def unapply(rsp: Response): Boolean =
        rsp.statusCode >= 500 && rsp.statusCode <= 599
    }

    object Retryable {
      // There are porbably some (linkerd-generated) failures that aren't
      // really retryable... For now just check if it's a failure.
      def unapply(rsp: Response): Boolean = Failure.unapply(rsp)
    }
  }

  val Idempotent: ResponseClassifier =
    ResponseClassifier.named("RetryableIdempotentHttp") {
      case ReqRep(Requests.Idempotent(), Return(Responses.Retryable()) | Throw(NonFatal(_))) =>
        ResponseClass.RetryableFailure

      case ReqRep(_, Return(Responses.Failure())) =>
        ResponseClass.NonRetryableFailure

      case ReqRep(_, Return(_)) =>
        ResponseClass.Success
    }

  val ReadOnly: ResponseClassifier =
    ResponseClassifier.named("RetryableReadOnlyHttp") {
      case ReqRep(Requests.ReadOnly(), Return(Responses.Retryable()) | Throw(NonFatal(_))) =>
        ResponseClass.RetryableFailure

      case ReqRep(_, Return(Responses.Failure())) =>
        ResponseClass.NonRetryableFailure

      case ReqRep(_, Return(_)) =>
        ResponseClass.Success
    }
}
