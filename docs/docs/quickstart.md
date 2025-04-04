# Quickstart

This 10-minute guide will show you how to run an entire Hermes environment, create topic and subscription and
publish some messages.

## Setting up the environment

Currently, there is only one way of setting up the environment - using docker.

#### Prerequisites

If you want to run hermes with docker, you need to have:

* [Docker Engine](https://docs.docker.com/engine/install/#server)
* [Docker Compose](https://docs.docker.com/compose/install/)
* curl
* some running receiver service (in this guide we'll use [webhook.site](http://webhook.site))

#### Setup

In order to run hermes in docker, you need to have the docker-compose file that can
be found [here](https://github.com/allegro/hermes/blob/master/docker/docker-compose.yml).

After downloading the file simply run this command inside the directory where the file is located:
```bash
docker-compose up
```
This may take up to several minutes as all docker images need to be downloaded from docker servers.

If you want to run kafka, zk, and schema-registry with specified image version (e.g. built for arm architecture)
set `CONFLUENT_IMAGES_TAG` env variable before running `docker-compose up`:

```bash
export CONFLUENT_IMAGES_TAG=7.2.2.arm64
docker-compose up
```

Use `VICTORIA_METRICS_IMAGES_TAG` env to change victoria-metrics and vmagent image version.

#### Checking the setup

Hermes console should be up and running on port 8090. Simply head [here](http://localhost:8090/).

#### Running a specific version

All hermes images can be found under these links:
* [hermes-management](https://hub.docker.com/repository/docker/allegro/hermes-management/)
* [hermes-frontend](https://hub.docker.com/repository/docker/allegro/hermes-frontend/)
* [hermes-consumers](https://hub.docker.com/repository/docker/allegro/hermes-consumers/)

If you want to run a specific hermes release simply add a given version to the image name inside the docker-compose file, for example:

```yaml
image: allegro/hermes-management:hermes-[specific version tag]
```

## Development

The default `docker-compose` setup will start all hermes modules (consumers, frontend, management), together
with its dependencies (Kafka, ZooKeeper, Schema Registry, VictoriaMetrics). To run a specific module with gradle/IntelliJ,
just comment out the module in `services` section of the `docker-compose.yml` file, and start the java process locally:

`./gradlew -p hermes-frontend run`

`./gradlew -p hermes-management run`

`./gradlew -p hermes-consumers run`

or use `Run/Debug Configurations` in IntelliJ.
The `application-local.yaml` configuration in each module is already adjusted to work with docker dependencies.

### Testing 

#### Unit tests

`./gradlew check`

#### Integration tests

`./gradlew integrationTest`

Optionally `confluentImagesTag` parameter can be provided to run tests with specified versions of
Kafka, ZooKeeper and SchemaRegistry. E.g. to run tests with images dedicated for arm64:

`./gradlew integrationTest -PconfluentImagesTag=7.2.2.arm64`


## Creating group and topic

Now you're ready to create a **topic** for publishing messages.

In Hermes messages are published on topics which are aggregated into **groups**.
So, you'll need to create a group first, let's name it `com.example.events`.

* head to Hermes Console: [link](http://localhost:8090/#/groups)
* click the blue plus button
* enter group name: `com.example.events`
* all the other information is required, but just enter whatever for now

At this point, you should see your group on the group list. Now let's add new `clicks` topic to our group:

* click the group header (direct link to com.example.events group: [link](http://localhost:8090/#/groups/com.example.events))
* click the blue plus button
* enter topic name: `clicks`
* enter some description and owner
* change content type to JSON - we don't want to add AVRO schema yet for the sake of simplicity

## Publishing and receiving messages

To receive messages that are published on topic you have to create a **subscription**. This is where you tell Hermes
where to send messages published on a topic. You can have many subscriptions on a single topic (in particular - none).

So let's create a `clicks-receiver` subscription:

* click the topic header (direct link to com.example.events.clicks group: [link](http://localhost:8090/#/groups/com.example.events/topics/com.example.events.clicks))
* click the blue plus button
* enter subscription name: `clicks-receiver`
* set rate limit, e.g: 100
* set the endpoint to which messages will be sent, in this example we can use `http://webhook.site/aa715639-e85d-43b4-9a29-ec46824021fe`
* enter some description and contact data

Now it's time for a grand finale. Let's publish a message on our topic (note that default Hermes publishing port is `8080`):

```bash
curl -v -d '{"id": 12345, "page": "main"}' http://10.10.10.10:8080/topics/com.example.events.clicks

< HTTP/1.1 201 Created
< Hermes-Message-Id: 66feaead-0685-491e-9c87-00f940ead2c9
< Content-Length: 0
< Date: Mon, 04 May 2015 02:18:23 GMT
```

(the first time you publish something you might see 408 Request Time-out status: a lot of machinery needs to warm up,
just hit retry)

Congratulations! The message should be delivered to your service or visible via e.g. [http://webhook.site/#!/aa715639-e85d-43b4-9a29-ec46824021fe/71377cf3-9076-4c06-b3ef-ec779170ce05/1](http://webhook.site/#!/aa715639-e85d-43b4-9a29-ec46824021fe/71377cf3-9076-4c06-b3ef-ec779170ce05/1).

## Stopping the system

To stop the system run this command in the directory where the docker-compose file is located:

```bash
docker-compose stop
```

To restart it run:

```bash
docker-compose restart
```

## Building your own docker image

You can build your own docker image for a specific module and later test it for example in `docker-compose.yml`.
Simply run this command from a hermes project root directory:

```bash
docker build --tag [your tag name] -f ./docker/latest/[hermes module]/Dockerfile .
```

For example:

```bash
docker build --tag hermes-management-test -f ./docker/latest/management/Dockerfile .
```

The built image can be tested directly in docker-compose. 
You need to replace image name with your tag name in ``docker-compose.yml``:

```yaml
[...]
management:
    image: [your tag name]
    ports:
      - "8090:8090"
    depends_on:
      - zk
      - kafka
[...]
```

Docker files for specific hermes modules can be found in `docker/latest` directory.