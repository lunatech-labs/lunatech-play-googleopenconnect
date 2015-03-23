Play 2.2.x library for Google Open Connect
==========================================

This is a simple library for enabling your Play 2.2.x scala application to authenticate using Google Open Connect

Application.conf configuration
------------------------------

# Google API
google.clientId=
google.secret=

# Additional configuration
For user verification, leave blank if users can be of any domain. Eg. `google.domain=google.com`.
google.domain=