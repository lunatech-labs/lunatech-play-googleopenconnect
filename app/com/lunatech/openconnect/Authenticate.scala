package com.lunatech.openconnect

import java.io.IOException
import java.math.BigInteger
import java.security.SecureRandom

import com.google.api.client.auth.oauth2.TokenResponseException
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeTokenRequest, GoogleCredential, GoogleTokenResponse}
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.oauth2.Oauth2
import com.google.api.services.oauth2.model.Tokeninfo
import com.google.inject.Inject
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}
import play.libs.Json
import play.mvc.Http

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class Authenticate @Inject()(configuration: Configuration, wsClient: WSClient)(implicit ec: ExecutionContext) {

  private val GOOGLE_CONF = "https://accounts.google.com/.well-known/openid-configuration"
  private val REVOKE_ENDPOINT = "revocation_endpoint"
  private val ERROR_GENERIC = "Something went wrong, please try again later"

  /**
    * Generate state for application based on known data
    */
  def generateState: String = new BigInteger(130, new SecureRandom()).toString(32)

  /**
    * Accepts an authResult['code'] as supplied by Google.
    * Returns authentication email and token parameters if successful, otherwise revokes user-granted permissions and returns an error.
    */
  def authenticateToken(code: String): Future[Either[AuthenticationResult, AuthenticationError]] = {

    val clientId = configuration.get[String]("google.clientId")
    val secret = configuration.get[String]("google.secret")
    val domains = configuration.get[Seq[String]]("google.domains")

    val ERROR_GOOGLE = configuration.get[String]("errors.authorization.googleDecline")
    val ERROR_MISMATCH_CLIENT = configuration.get[String]("errors.authorization.clientIdMismatch")
    val ERROR_MISMATCH_DOMAIN = configuration.get[String]("errors.authorization.domainMismatch")

    try {

      val transport: NetHttpTransport = new NetHttpTransport()
      val jsonFactory: JacksonFactory = new JacksonFactory()

      val tokenResponse: GoogleTokenResponse = new GoogleAuthorizationCodeTokenRequest(
        transport, jsonFactory, clientId, secret, code, "postmessage"
      ).execute()

      val credential: GoogleCredential = new GoogleCredential.Builder()
        .setJsonFactory(jsonFactory)
        .setTransport(transport)
        .setClientSecrets(clientId, secret).build()
        .setFromTokenResponse(tokenResponse)

      val oauth2: Oauth2 = new Oauth2.Builder(
        transport, jsonFactory, credential).setApplicationName("Lunatech Google Openconnect").build()

      val tokenInfo: Tokeninfo = oauth2.tokeninfo().setAccessToken(credential.getAccessToken).execute()

      if (tokenInfo.containsKey("error")) {
        Logger.error(s"Authorizationtoken has been denied by Google")
        revokeUser(credential.getAccessToken, AuthenticationServiceError(ERROR_GOOGLE))
      } else if (!tokenInfo.getIssuedTo.equals(clientId)) {
        Logger.error(s"client_id doesn't match expected client_id")
        revokeUser(credential.getAccessToken, TokenClientMismatchError(ERROR_MISMATCH_CLIENT))
      } else if (domains.nonEmpty && domains.forall(domain => !tokenInfo.getEmail.endsWith(domain))) {
        Logger.error(s"domain doesn't match one of the expected domains")
        revokeUser(credential.getAccessToken, TokenDomainMismatchError(ERROR_MISMATCH_DOMAIN))
      } else {
        Future(Left(AuthenticationResult(tokenInfo.getEmail, tokenResponse.toString)))
      }
    } catch {
      case tre: TokenResponseException =>
        Logger.error("Unable to request authorization to Google " + tre)
        Future(Right(TokenResponseError(ERROR_GENERIC)))
      case ioe: IOException =>
        Logger.error("Unable to request authorization to Google " + ioe)
        Future(Right(TokenIOError(ERROR_GENERIC)))
    }
  }

  private def revokeUser(token: String, reason: AuthenticationError): Future[Either[AuthenticationResult, AuthenticationError]] = {
    wsClient.url(getRevokeEndpoint).addQueryStringParameters("token" -> token).get.map { response =>
      response.status match {
        case Http.Status.OK =>
          Logger.info("User successfully revoked")
          Right(reason)
        case _ =>
          Logger.info("ERROR revoking user access")
          Right(UserRevokeError(ERROR_GENERIC))
      }
    }
  }

  private def getRevokeEndpoint: String = Json.parse(Source.fromURL(GOOGLE_CONF).mkString).get(REVOKE_ENDPOINT).asText()

  case class AuthenticationResult(email: String, token: String)

}
