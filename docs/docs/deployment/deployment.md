# Deployment

This section covers basic operational aspects of deploying Hermes. For more on configuring Hermes read:

* [how to connect to Kafka and Zookeeper](/configuration/kafka-and-zookeeper)
* [how to fine tune Frontend](/configuration/frontend-tuning)
* [how to fine tune Consumers](/configuration/consumers-tuning)
* [how to configure Console](/configuration/console)
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

All Hermes Java modules require **Java 8** to work. Hermes Console has no external dependencies.

## Passing environment variables

All Java modules share the same bundling strategy: Gradle distZips. In order to pass any command line options to
executables use:

* `HERMES_<module name>_OPTS` for application options
* `JAVA_OPTS` for Java specific options

for example:

```bash
export HERMES_FRONTEND_OPTS="-Dfrontend.port=8090"
export JAVA_OPTS="-Xmx2g"
```

## Frontend and Consumers

### External configuration

Hermes Frontend and Consumers modules use [Netflix Archaius](https://github.com/Netflix/archaius/) to manage configuration.

To read external configuration from any URL (local file or remote HTTP source), specify its location in system property:

```bash
export HERMES_FRONTEND_OPTS="-Darchaius.configurationSource.additionalUrls=file:///opt/hermes/conf/frontend.properties"
export HERMES_CONSUMERS_OPTS="-Darchaius.configurationSource.additionalUrls=file:///opt/hermes/conf/consumers.properties"
```

Configuration is stored in Java properties format.

### Overwriting configuration using ENV

It is possible to overwrite any configuration variable using environment variable:

```bash
export HERMES_FRONTEND_OPTS="-D<configuration-option>=<value>"
```

for example:

```bash
export HERMES_FRONTEND_OPTS="-Dfrontend.port=8090 -Dfrontend.idle.timeout=30"
```

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

### Overwriting configuration using ENV

```bash
export HERMES_MANAGEMENT_OPTS="-D<configuration-option>=<value>"
```

```bash
export HERMES_MANAGEMENT_OPTS="-Dserver.port=8070"
```

## Console

Hermes Console is a simple Single Page Application served using NodeJS. It accepts two arguments:

* `-p` or `HERMES_CONSOLE_PORT` env variable to specify port (default: 8000)
* `-c` or `HERMES_CONSOLE_CONFIG` env variable to specify configuration file (default: `./config.json`)

The `config.json` file is mandatory, Hermes Console will crash when unable to read it. See
[configuring Hermes Console](/configuration/console) section for more information.

Hermes Console has no dependencies and will run out of box on Linux machines. To run it, use provided script:

```
./run.sh -p 8000 -c /etc/hermes-console/config.json
```
