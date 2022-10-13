Play 2.8.x library for Google Open Connect
==========================================

This is a simple library for enabling your Play Scala application to authenticate using Google Open Connect.

build.sbt
---------
```
resolvers += "Lunatech Artifactory" at "https://artifactory.lunatech.com/artifactory/releases-public"

libraryDependencies ++= Seq(
  "com.lunatech" %% "play-googleopenconnect" % "2.9.0"
)
```

application.conf configuration
------------------------------

# Google API
```
google.clientId=
google.secret=
```

# Additional configuration
For user verification, leave empty if users can be of any domain.  
`google.domains=["mydomain.com"]`

# Administrators
For users that are an admin for your application you can specify an array of emails
`administrators=["developer@lunatech.com","admin@lunatech.com"]`

# Custom error messages
Optionally you can define some of the error messages in your `application.conf`. Setting any of these overrides the default response.  
```
errors.authorization.googleDecline=
errors.authorization.clientIdMismatch=
errors.authorization.domainMismatch=
```

Example
-------
Full code example can be found at [Github](https://github.com/lunatech-labs/lunatech-kitchen-sink)

Implement a trait which you will use for your controllers, defining methods below when a user is not authenticated or authorized:
```
trait Secured extends GoogleSecured {
    override def onUnauthorized(request: RequestHeader): Result = Results.Redirect(routes.Application.login())
   
    override def onForbidden(request: RequestHeader): Result = Results.Forbidden("YOU ARE NOT ADMIN!!!")
}
```

Optionally override given methods when for example you want to retrieve admin users from a database
```
    override def isAdmin(email: String): Boolean =
        DB.isAdmin(email))
    }
```

Next use the methods in your controller
```
class TestController @Inject()(val configuration: Configuration,
                               val environment: Environment) extends Controller with Secured {

  def adminAction = adminAction { implicit request =>
      Ok("adminAction" + request.email)
  }

  def asyncAdminAction = adminAction.async { implicit request =>
      Future.successful(Ok("asyncAdminAction" + request.email))
  }

  def authenticatedAction = userAction { implicit request =>
      Ok("authenticatedAction" + request.email)
  }

  def asyncAuthenticatedAction = userAction.async { implicit request =>
      Future.successful(Ok("asyncAuthenticatedAction" + request.email))
  }
}
```

And create a controller with methods for login, authenticate and logout
```
class Authentication @Inject()(configuration: Configuration, environment: Environment, auth: Authenticate) extends Controller {

  /**
    * Login page.
    */
  def login = Action { implicit request =>
    if (environment.mode == Mode.Prod) {
      Ok(views.html.login(clientId)(request.flash))
    } else {
      Redirect(routes.Application.index()).withSession("email" -> "developer@lunatech.com")
    }
  }

  def authenticate(code: String) = Action.async {
    val response = auth.authenticateToken(code)

    response.map {
      case Left(parameters) => Redirect(routes.Application.index()).withSession(parameters.toArray: _*)
      case Right(message) => Redirect(routes.Authentication.login()).withNewSession.flashing("error" -> message.toString())
    }
  }

  /**
    * Logout and clean the session.
    */
  def logout = Action {
    Redirect(routes.Authentication.login()).withNewSession.flashing("success" -> "You've been logged out")
  }
```
