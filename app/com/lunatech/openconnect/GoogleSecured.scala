package com.lunatech.openconnect

import play.api.mvc._
import play.api.{Configuration, Logger}

import scala.concurrent.Future
import scala.util.Try

trait GoogleSecured extends Controller {
  val configuration: Configuration

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
  def adminAction: ActionBuilder[UserRequest] = userAction andThen new AdminAction(configuration)

  /**
    * Action for authenticated users.
    */
  def userAction: UserAction = new UserAction

  /**
    * Check if email is in the list of administrators
    */
  def isAdmin(email: String): Boolean = Try {
    configuration.underlying.getStringList("administrators").contains(email)
  }.getOrElse {
    Logger.error("No administrators defined!!!")
    false
  }

  class UserRequest[A](val email: String, request: Request[A]) extends WrappedRequest[A](request)

  class UserAction extends ActionBuilder[UserRequest] with ActionRefiner[Request, UserRequest] {
    def refine[A](request: Request[A]): Future[Either[Result, UserRequest[A]]] = Future.successful {
      request.session.get("email") match {
        case Some(email) => Right(new UserRequest(email, request))
        case None => Left(onUnauthorized(request))
      }
    }
  }

  class AdminAction(configuration: Configuration) extends ActionFilter[UserRequest] {
    def filter[A](request: UserRequest[A]): Future[Option[Result]] = Future.successful {
      if (isAdmin(request.email)) None
      else Some(onForbidden(request))
    }
  }

}
