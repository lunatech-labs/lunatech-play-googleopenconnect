package com.lunatech.openconnect

import com.google.inject.Inject
import play.api.Logging
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait GoogleSecured extends Secured {

  private val parser: BodyParser[AnyContent] = controllerComponents.parsers.defaultBodyParser

  override def userAction: ActionBuilder[UserRequest, AnyContent] = new UserAction(parser)

  class UserAction @Inject()(val parser: BodyParser[AnyContent])(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[UserRequest, AnyContent] with ActionRefiner[Request, UserRequest] {

    def refine[A](request: Request[A]): Future[Either[Result, UserRequest[A]]] = Future.successful {
      request.session.get("email") match {
        case Some(email) => Right(new UserRequest(email, request))
        case None => Left(onUnauthorized(request))
      }
    }
  }
}
