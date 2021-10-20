# Quickstart

This 10-minute guide will show you how to run an entire Hermes environment, create topic and subscription and
publish some messages.

## Setting up the environment
There are two ways of setting up the environment: using vagrant or docker.

### Vagrant

#### Prerequisites

In order to go through this tutorial you need to have:

* [Vagrant 1.7.3+](https://www.vagrantup.com/)
* [VirtualBox](https://www.virtualbox.org/) (4.0.x, 4.1.x, 4.2.x, 4.3.x, 5.0.x)
* curl
* some running receiver service (in this guide we'll use [webhook.site](http://webhook.site))

#### Setup

As described in [architecture](overview/architecture.md) section, Hermes consists of multiple modules and requires Kafka
and Zookeeper to run. To make this easy, we prepared a Vagrant file.

```bash
git clone https://github.com/allegro/hermes.git
cd hermes
vagrant up
```

If you want to run specific version of Hermes, simply checkout a tag:

```bash
git checkout hermes-{version}
```

#### Checking the setup

If the system is running, you should see Hermes Console when visiting Vagrant public IP in the browser. Just head to
[http://10.10.10.10/](http://10.10.10.10/).

### Docker

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

## Creating group and topic

Now you're ready to create a **topic** for publishing messages.

In Hermes messages are published on topics which are aggregated into **groups**.
So, you'll need to create a group first, let's name it `com.example.events`.

* head to Hermes Console: [Vagrant](http://10.10.10.10/#/groups) / [Docker](http://localhost:8090/#/groups)
* click the blue plus button
* enter group name: `com.example.events`
* all the other information is required, but just enter whatever for now

At this point, you should see your group on the group list. Now let's add new `clicks` topic to our group:

* click the group header (direct link to com.example.events group: [Vagrant](http://10.10.10.10/#/groups/com.example.events) /
 [Docker](http://localhost:8090/#/groups/com.example.events) )
* click the blue plus button
* enter topic name: `clicks`
* enter some description
* change content type to JSON - we don't want to add AVRO schema yet for the sake of simplicity

## Publishing and receiving messages

To receive messages that are published on topic you have to create a **subscription**. This is where you tell Hermes
where to send messages published on a topic. You can have many subscriptions on a single topic (in particular - none).

So let's create a `clicks-receiver` subscription:

* click the topic header (direct link to com.example.events.clicks group: 
[Vagrant](http://10.10.10.10/#/groups/com.example.events/topics/com.example.events.clicks) / [Docker](http://localhost:8090/#/groups/com.example.events/topics/com.example.events.clicks))
* click the blue plus button
* enter subscription name: `clicks-receiver`
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

### Vagrant

To stop the virtual machine run:

```bash
vagrant halt
```

Run it again with:

```bash
vagrant up
```

Destroy the VM with:

```bash
vagrant destroy
```

### Docker

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
      - graphite
[...]
```

Docker files for specific hermes modules can be found in `docker/latest` directory.