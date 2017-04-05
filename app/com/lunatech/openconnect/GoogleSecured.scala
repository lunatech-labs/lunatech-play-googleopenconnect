package com.lunatech.openconnect

import play.api.mvc._
import play.api.{Configuration, Environment}

import scala.concurrent.Future

trait GoogleSecured {
  val configuration: Configuration
  val environment: Environment

  /**
    * Redirect to login if the user in not authorized.
    */
  def onUnauthorized(request: RequestHeader): Result

  /**
    * Redirect to login if the user in not authorized as admin.
    */
  def onForbidden(request: RequestHeader): Result

  /**
    * Async action for authenticated admin users.
    */
  def IsAdminAsync(f: String => Request[AnyContent] => Future[Result]): EssentialAction =
    IsAuthenticatedAsync { user =>
      request =>
        if (isAdmin(user)) f(user)(request)
        else Future.successful(onForbidden(request))
    }

  /**
    * Async action for authenticated users.
    */
  def IsAuthenticatedAsync(f: String => Request[AnyContent] => Future[Result]): EssentialAction =
    Security.Authenticated(username, onUnauthorized) { user =>
      Action.async(request => f(user)(request))
    }

  /**
    * Action for authenticated admin users.
    */
  def IsAdmin(f: String => Request[AnyContent] => Result): EssentialAction =
    IsAuthenticated { user =>
      request =>
        if (isAdmin(user)) f(user)(request)
        else onForbidden(request)
    }

  /**
    * Action for authenticated users.
    */
  def IsAuthenticated(f: String => Request[AnyContent] => Result): EssentialAction =
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }

  /**
    * Retrieve the connected user email.
    */
  private def username(request: RequestHeader) = request.session.get("email")

  /**
    * Check if email is in the list of administrators
    */
  private def isAdmin(user: String) = configuration.getStringList("administrators").get.contains(user)
}
