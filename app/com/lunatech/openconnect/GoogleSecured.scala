package com.lunatech.openconnect

import com.google.inject.Inject
import play.api.mvc._
import play.api.{Configuration, Logger}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait GoogleSecured {
  val configuration: Configuration
  val controllerComponents: ControllerComponents

  private implicit val executionContext: ExecutionContext = controllerComponents.executionContext

  private val parser: BodyParser[AnyContent] = controllerComponents.parsers.defaultBodyParser

  /**
    * Redirect to login if the user in not authorized.
    */
  def onUnauthorized[A](request: Request[A]): Result = Results.Unauthorized("you are not authenticated")

  /**
    * Redirect to login if the user in not authorized as admin.
    */
  def onForbidden[A](request: Request[A]): Result = Results.Forbidden("you are not an admin!")

  /**
    * Action for authenticated admin users.
    */
  def adminAction: ActionBuilder[UserRequest, AnyContent] = userAction andThen new AdminAction(configuration)

  /**
    * Action for authenticated users.
    */
  def userAction: UserAction = new UserAction(parser)

  /**
    * Check if email is in the list of administrators
    */
  def isAdmin(email: String): Boolean = Try {
    configuration.get[Seq[String]]("administrators").contains(email)
  }.getOrElse {
    Logger.error("No administrators defined!!!")
    false
  }

  class UserRequest[A](val email: String, request: Request[A]) extends WrappedRequest[A](request)

  class UserAction @Inject()(val parser: BodyParser[AnyContent])(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[UserRequest, AnyContent] with ActionRefiner[Request, UserRequest] {

    def refine[A](request: Request[A]): Future[Either[Result, UserRequest[A]]] = Future.successful {
      request.session.get("email") match {
        case Some(email) => Right(new UserRequest(email, request))
        case None => Left(onUnauthorized(request))
      }
    }
  }

  class AdminAction(configuration: Configuration)(implicit val executionContext: ExecutionContext)
    extends ActionFilter[UserRequest] {

    def filter[A](request: UserRequest[A]): Future[Option[Result]] = Future.successful {
      if (isAdmin(request.email)) None
      else Some(onForbidden(request))
    }
  }

}
