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

# Custom error messages
Optionally you can define some of the error messages in your application.conf. Setting any of these overrides the default response.  
`errors.authorization.googleDecline=`
`errors.authorization.clientIdMismatch=`
`errors.authorization.domainMismatch=`