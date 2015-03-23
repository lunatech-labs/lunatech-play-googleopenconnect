package com.lunatech.openconnect

import java.io.IOException
import java.math.BigInteger
import java.security.SecureRandom

import com.google.api.client.auth.oauth2.TokenResponseException
import com.google.api.client.googleapis.auth.oauth2.{GoogleCredential, GoogleAuthorizationCodeTokenRequest, GoogleTokenResponse}
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.oauth2.Oauth2
import com.google.api.services.oauth2.model.Tokeninfo

import play.api.Play
import play.api.libs.ws.WS
import play.api.Play.current
import play.libs.Json

import play.api.libs.concurrent.Execution.Implicits._
import play.mvc.Http

import scala.concurrent.Future
import scala.io.Source

object Authenticate {

  val GOOGLE_CLIENTID = "google.clientId"
  val GOOGLE_DOMAIN = "google.domain"
  val GOOGLE_SECRET = "google.secret"

  val GOOGLE_CONF = "https://accounts.google.com/.well-known/openid-configuration"
  val REVOKE_ENDPOINT = "revocation_endpoint"

  def generateState: String = new BigInteger(130, new SecureRandom()).toString(32)

  def authenticateToken(code: String, id_token: String, accessToken: String): Future[Either[Seq[(String, String)], AuthenticationError]] = {

    val gPlusId: String = id_token

    val clientId: String = Play.configuration.getString("google.clientId").get
    val secret: String = Play.configuration.getString("google.secret").get
    val domain: String = Play.configuration.getString("google.domain").get

    try {
      val tokenResponse: GoogleTokenResponse = new GoogleAuthorizationCodeTokenRequest(
        new NetHttpTransport(), new JacksonFactory(), clientId, secret, code, "postmessage"
      ).execute()

      val credential: GoogleCredential = new GoogleCredential.Builder()
        .setJsonFactory(new JacksonFactory())
        .setTransport(new NetHttpTransport())
        .setClientSecrets(clientId, secret).build()
        .setFromTokenResponse(tokenResponse)

      val oauth2: Oauth2 = new Oauth2.Builder(
        new NetHttpTransport(), new JacksonFactory, credential).build()

      val tokenInfo: Tokeninfo = oauth2.tokeninfo().setAccessToken(credential.getAccessToken).execute()

      if(tokenInfo.containsKey("error")) {
        play.Logger.error(s"Authorizationtoken has been denied by Google")
        revokeUser(accessToken, AuthenticationServiceError("Unable to authorize account, please try again later."))
      } else if(!tokenInfo.getIssuedTo.equals(clientId)) {
        play.Logger.error(s"client_id doesn't match expected client_id")
        revokeUser(accessToken, TokenClientMismatchError("Something went wrong, please try again later."))
      } else if(!domain.isEmpty && !tokenInfo.getEmail.endsWith(domain)) {
        play.Logger.error(s"domain doesn't match expected domain")
        revokeUser(accessToken, TokenDomainMismatchError("Something went wrong, please try again later."))
      } else {
        Future(Left(Seq("email" -> tokenInfo.getEmail, "token" -> tokenResponse.toString())))
      }
    } catch {
      case tre: TokenResponseException => {
        play.Logger.error("Unable to request authorization to Google " + tre)
        revokeUser(accessToken, TokenResponseError("Something went wrong, please try again later."))
      }
      case ioe: IOException => {
        play.Logger.error("Unable to request authorization to Google " + ioe)
        revokeUser(accessToken, TokenIOError("Something went wrong, please try again later."))
      }
    }
  }

  private def getRevokeEndpoint: String = Json.parse(Source.fromURL(GOOGLE_CONF).mkString).get(REVOKE_ENDPOINT).asText()

  private def revokeUser(token: String, reason: AuthenticationError): Future[Either[Seq[(String, String)], AuthenticationError]] = {
    WS.url(getRevokeEndpoint).withQueryString("token" -> token).get().map {
      response => response.status match {
        case Http.Status.OK => {
          play.Logger.info("User succesfully revoked")
          Right(reason)
        }
        case _ => {
          play.Logger.info("ERROR revoking user access")
          Right(UserRevokeError("Something went wrong, please try again later."))
        }
      }
    }
  }
}