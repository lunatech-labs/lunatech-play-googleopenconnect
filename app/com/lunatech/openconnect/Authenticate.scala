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
import play.api.{Configuration, Logger}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WSClient
import play.libs.Json
import play.mvc.Http

import scala.concurrent.Future
import scala.io.Source

class Authenticate @Inject()(configuration: Configuration, wsClient: WSClient) {

  private val GOOGLE_CONF = "https://accounts.google.com/.well-known/openid-configuration"
  private val REVOKE_ENDPOINT = "revocation_endpoint"
  private val ERROR_GENERIC = "Something went wrong, please try again later"

  /**
    * Generate state for application based on known data
    */
  def generateState: String = new BigInteger(130, new SecureRandom()).toString(32)

  /**
    * Accepts an authResult['code'], authResult['id_token'], and authResult['access_token'] as supplied by Google.
    * Returns authentication email and token parameters if successful, otherwise revokes user-granted permissions and returns an error.
    */
  def authenticateToken(code: String, id_token: String, accessToken: String): Future[Either[Seq[(String, String)], AuthenticationError]] = {

    val clientId = configuration.getString("google.clientId").get
    val secret = configuration.getString("google.secret").get
    val domain = configuration.getString("google.domain").get

    val ERROR_GOOGLE = configuration.getString("errors.authorization.googleDecline").getOrElse("Unable to authorize account, please try again later.")
    val ERROR_MISMATCH_CLIENT = configuration.getString("errors.authorization.clientIdMismatch").getOrElse(ERROR_GENERIC)
    val ERROR_MISMATCH_DOMAIN = configuration.getString("errors.authorization.domainMismatch").getOrElse(s"Please use a '$domain' account.")

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
        revokeUser(accessToken, AuthenticationServiceError(ERROR_GOOGLE))
      } else if (!tokenInfo.getIssuedTo.equals(clientId)) {
        Logger.error(s"client_id doesn't match expected client_id")
        revokeUser(accessToken, TokenClientMismatchError(ERROR_MISMATCH_CLIENT))
      } else if (!domain.isEmpty && !tokenInfo.getEmail.endsWith(domain)) {
        Logger.error(s"domain doesn't match expected domain")
        revokeUser(accessToken, TokenDomainMismatchError(ERROR_MISMATCH_DOMAIN))
      } else {
        Future(Left(Seq("email" -> tokenInfo.getEmail, "token" -> tokenResponse.toString)))
      }
    } catch {
      case tre: TokenResponseException =>
        Logger.error("Unable to request authorization to Google " + tre)
        revokeUser(accessToken, TokenResponseError(ERROR_GENERIC))
      case ioe: IOException =>
        Logger.error("Unable to request authorization to Google " + ioe)
        revokeUser(accessToken, TokenIOError(ERROR_GENERIC))
    }
  }

  private def revokeUser(token: String, reason: AuthenticationError): Future[Either[Seq[(String, String)], AuthenticationError]] = {
    wsClient.url(getRevokeEndpoint).withQueryString("token" -> token).get.map { response =>
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
}