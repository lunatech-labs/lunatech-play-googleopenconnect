package com.lunatech.openconnect

sealed trait AuthenticationError {
   val message: String

   override def toString: String = message
}

case class AuthenticationServiceError(message: String) extends AuthenticationError {}
case class TokenUserMismatchError(message: String) extends AuthenticationError
case class TokenClientMismatchError(message: String) extends AuthenticationError
case class TokenDomainMismatchError(message: String) extends AuthenticationError
case class TokenResponseError(message: String) extends AuthenticationError
case class TokenIOError(message: String) extends AuthenticationError
case class UserRevokeError(message: String) extends AuthenticationError
