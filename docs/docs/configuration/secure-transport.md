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
It is possible to change it using the custom `consumer.ssl.truststore.*` options:

Option                           | Description
-------------------------------- | --------------------------
consumer.ssl.truststore.location | path to custom trust store
consumer.ssl.truststore.format   | trust store format
consumer.ssl.truststore.password | password to trust store
