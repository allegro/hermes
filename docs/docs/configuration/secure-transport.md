# Secure transport

All Hermes modules support secure transport in their own way:

* Frontend can accept SSL (or Http/2) traffic
* Consumers can send messages via SSL
* Management operations can be secured using SSL

>
> Currently we will describe only Consumers SSL configuration.
>

## Consumers SSL

Consumers by default support sending traffic to `https` endpoints. They use JRE trust store to verify the certificates.
It is possible to change it using standard `javax.net.ssl.*` options:

Option                           | Description
-------------------------------- | --------------------------
javax.net.ssl.trustStore         | path to custom trust store
javax.net.ssl.trustStoreType     | trust store format
javax.net.ssl.trustStorePassword | password to trust store
