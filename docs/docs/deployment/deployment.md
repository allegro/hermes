# Deployment

This section covers basic operational aspects of deploying Hermes. For more on configuring Hermes read:

* [how to connect to Kafka and Zookeeper](/configuration/kafka-and-zookeeper)
* [how to fine tune Frontend](/configuration/frontend-tuning)
* [how to fine tune Consumers](/configuration/consumers-tuning)
* [how to publish metrics](/configuration/metrics)

## Dependencies

As the [architecture overview](/overview/architecture) states, there are two systems that are required to run
Hermes:

* **Kafka**
* **Zookeeper**

In our opinion it is best practice, to run them on separate hosts, so Hermes does not affect them.

## Scalability

Each module is a stateless application. There can be as many of them running in parallel as it is required. For best
performance and easy maintenance, each Hermes module should also be deployed on separate host.

## Requirements

All Hermes modules require **Java 8** to work.

## Frontend and Consumers

### External configuration

Hermes Frontend and Consumers modules use [Netflix Archaius](https://github.com/Netflix/archaius/) to manage configuration.

To read external configuration from any URL (local file or remote HTTP source), specify its location in system property:

```
-Darchaius.configurationSource.additionalUrls=file:///opt/hermes/conf/frontend.properties
```

Configuration is stored in Java properties format.

### Java options

It is advised to run Hermes Frontend and Consumers with G1 garbage collector and at least 1GB heap:

```
-XX:+UseG1GC -Xms1g
```

## Management

### External configuration

Management being Spring Boot application, shares the same options to provide additional configuration. The most basic way
to provide external configuration file is to export an environment variable:

```
SPRING_CONFIG_LOCATION="file:///opt/hermes/conf/management.properties"
```
