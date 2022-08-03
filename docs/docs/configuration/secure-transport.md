# Secure transport

All Hermes modules support secure transport in their own way:

* Frontend can accept SSL (or Http/2) traffic
* Consumers can send messages via SSL
* Management operations can be secured using SSL

>
> Currently we will describe only Consumers SSL configuration.
>

## Consumers SSL

Consumers by default support sending traffic to `https` endpoints.
By default hermes uses JRE trust store (located in `$JAVA_HOME/jre/lib/security/`) to verify the certificates.
It loads file `jssecacerts` if exists, otherwise it loads file `cacerts`.
In case of lack of both files the `FileNotFoundException` is thrown.

It is possible to use custom trust store by setting the property `consumer.ssl.truststoreSource` to `provided`, which by default is set to `jre`.
In case of provided trust store it is required to specify additional properties:

Option                           | Description                | Default value
-------------------------------- | -------------------------- | -----------------------
consumer.ssl.truststoreLocation  | path to custom trust store (it could be the classpath or a path in a file system) | `classpath:server.truststore`
consumer.ssl.truststoreFormat    | trust store format | `JKS`
consumer.ssl.truststorePassword  | password to trust store | `password`
