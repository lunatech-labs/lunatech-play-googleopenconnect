Play 2.5.x library for Google Open Connect
==========================================

This is a simple library for enabling your Play 2.5.x scala application to authenticate using Google Open Connect

Application.conf configuration
------------------------------

# Google API
`google.clientId=`
`google.secret=`

# Additional configuration
For user verification, leave blank if users can be of any domain.  
`google.domain=mydomain.com`

# Administrators
For users that are an admin for your application you can specify an array of emails
`administrators=["developer@lunatech.com","admin@lunatech.com"]`

# Custom error messages
Optionally you can define some of the error messages in your application.conf. Setting any of these overrides the default response.  
`errors.authorization.googleDecline=`
`errors.authorization.clientIdMismatch=`
`errors.authorization.domainMismatch=`

Example
-------
Implement a trait which you will use for your controllers, defining methods below when a user is not authorized:
```
trait Secured extends GoogleSecured {
    override def onUnauthorized(request: RequestHeader): Result = Results.Redirect(routes.Application.login())
   
    override def onForbidden(request: RequestHeader): Result = Results.Forbidden("YOU ARE NOT ADMIN!!!")
}
```

Optionally override given methods when for example you want to retrieve admin users from a database
```
    override def IsAdmin(f: String => Request[AnyContent] => Result): EssentialAction =
        IsAuthenticated { user =>
            request =>
                if(DB.isAdmin(user)) f(user)(request)
                else onForbidden(request)
         }
```

Next use the methods in your controller
```
class TestController @Inject()(val configuration: Configuration,
                               val environment: Environment) extends Controller with Secured {

  def adminAction = IsAdmin { implicit user =>
    implicit request =>
      Ok("adminAction")
  }

  def asyncAdminAction = IsAdminAsync { implicit user =>
    implicit request =>
      Future.successful(Ok("asyncAdminAction"))
  }

  def authenticatedAction = IsAuthenticated { implicit user =>
    implicit request =>
      Ok("authenticatedAction")
  }

  def asyncAuthenticatedAction = IsAuthenticatedAsync { implicit user =>
    implicit request =>
      Future.successful(Ok("asyncAuthenticatedAction"))
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
      val clientId: String = configuration.getString("google.clientId").get
      Ok(views.html.login(clientId)(request.flash)).withSession("state" -> auth.generateState)
    } else {
      Redirect(routes.Application.index()).withSession("email" -> "developer@lunatech.com")
    }
  }

  def authenticate(code: String, idToken: String, accessToken: String) = Action.async {
    val response = auth.authenticateToken(code, idToken, accessToken)

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