package com.lunatech.openconnect

import play.api.mvc._
import play.api.{Configuration, Logging}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

private[openconnect] trait Secured extends Logging {
  val configuration: Configuration
  val controllerComponents: ControllerComponents

  protected implicit val executionContext: ExecutionContext = controllerComponents.executionContext

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
  def adminAction: ActionBuilder[UserRequest, AnyContent] = userAction andThen adminAction

  /**
    * Action for authenticated admin users or for email with specific email
    */
  def adminOrEmployeeAction(email: String): ActionBuilder[UserRequest, AnyContent] = userAction andThen adminOrEmployeeFilter(email)

  /**
    * Action for authenticated users.
    */
  def userAction: ActionBuilder[UserRequest, AnyContent]

  /**
    * Check if email is in the list of administrators
    */
  def isAdmin(email: String): Boolean = Try {
    configuration.get[Seq[String]]("administrators").contains(email)
  }.getOrElse {
    logger.error("No administrators defined!!!")
    false
  }

  class UserRequest[A](val email: String, request: Request[A]) extends WrappedRequest[A](request)

  def adminFilter(implicit ec: ExecutionContext) = new ActionFilter[UserRequest] {
    def executionContext: ExecutionContext = ec
    def filter[A](request: UserRequest[A]): Future[Option[Result]] = Future.successful {
      if (isAdmin(request.email)) None
      else Some(onForbidden(request))
    }
  }

  def adminOrEmployeeFilter(employeeEmail: String)(implicit ec: ExecutionContext) = new ActionFilter[UserRequest] {
    def executionContext: ExecutionContext = ec
    def filter[A](request: UserRequest[A]): Future[Option[Result]] = Future.successful {
      if (isAdmin(request.email) || request.email == employeeEmail) None
      else Some(onForbidden(request))
    }
  }
}
