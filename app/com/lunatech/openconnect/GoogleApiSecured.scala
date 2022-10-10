package com.lunatech.openconnect

import com.google.inject.Inject
import play.api.Configuration
import play.api.http.{SecretConfiguration, SessionConfiguration}
import play.api.mvc.{ActionBuilder, ActionRefiner, AnyContent, BodyParser, DefaultJWTCookieDataCodec, DefaultSessionCookieBaker, JWTCookieDataCodec, Request, Result, Results}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class APISessionCookieBaker @Inject()(configuration: Configuration,
                                      secretConfiguration: SecretConfiguration,
                                      sessionsConfiguration: SessionConfiguration) extends DefaultSessionCookieBaker {
  private val jwtConf = sessionsConfiguration.jwt.copy(expiresAfter = configuration.get[Option[FiniteDuration]]("session.ttl"))
  override val jwtCodec: JWTCookieDataCodec = DefaultJWTCookieDataCodec(secretConfiguration, jwtConf)
}

trait GoogleApiSecured extends Secured {
  val apiSessionCookieBaker: APISessionCookieBaker
  private val parser: BodyParser[AnyContent] = controllerComponents.parsers.defaultBodyParser

  override def userAction: ActionBuilder[UserRequest, AnyContent] = new UserAction(parser)

  class UserAction @Inject()(val parser: BodyParser[AnyContent])(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[UserRequest, AnyContent] with ActionRefiner[Request, UserRequest] {

    def refine[A](request: Request[A]): Future[Either[Result, UserRequest[A]]] = Future.successful {
      /**
        * We expect a client to send a request with the JWT in the following header:
        * Authorization: Bearer <token>
        */
      request.headers.get("Authorization") match {
        case Some(authHeader) =>
          val decodedToken = apiSessionCookieBaker.jwtCodec.decode(authHeader.replaceFirst("Bearer ", ""))
          decodedToken.get("email") match {
            case Some(email) =>
              if (email.equalsIgnoreCase("0"))
                Left(Results.Unauthorized("Email is invalid"))
              else
                Right(new UserRequest(email, request))
            case _ => Left(onUnauthorized(request))
          }
        case None => Left(onUnauthorized(request))
      }
    }
  }
}
