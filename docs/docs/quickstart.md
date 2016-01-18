# Quickstart

This 10-minute guide will show you how to run an entire Hermes environment, create topic and subscription and
publish some messages.

## Prerequisities

In order to go through this tutorial you need to have:

* [Vagrant 1.7.3+](https://www.vagrantup.com/)
* [VirtualBox](https://www.virtualbox.org/) (4.0.x, 4.1.x, 4.2.x, 4.3.x, 5.0.x)
* curl
* some running receiver service (in this guide we'll use [RequestBin](http://requestb.in))

## Setting up an environment

As described in [architecture](/overview/architecture) section, Hermes consists of multiple modules and requires Kafka
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

## Checking the setup

If the system is running, you should be able to invoke some management endpoint by making a call to Hermes REST API:

```bash
curl http://10.10.10.10:8090/topics
[]
```

## Creating group and topic

Now you're ready to create a **topic** for publishing messages.

In Hermes messages are published on topics which are aggregated into **groups**.
So, you'll need to create a group first, let's name it `com.example.events`:

```bash
curl -d '{"groupName": "com.example.events"}' -H "Content-Type: application/json" http://10.10.10.10:8090/groups
```

Now we can list groups to ensure it's been created:

```bash
curl http://10.10.10.10:8090/groups
["com.example.events"]
```

Okay, now it's time to create a topic in our group. Imagine you want to track user clicks, so we name it `com.example.events.clicks`:

```
curl -d '{"name": "com.example.events.clicks"}' -H "Content-Type: application/json" http://10.10.10.10:8090/topics
```

And list topics:

```bash
curl http://10.10.10.10:8090/topics
["com.example.events.clicks"]
```

Voila!

## Publishing and receiving messages

To receive messages that are published on topic you have to create a **subscription**. This is where you tell Hermes
where to send messages published on a topic. You can have many subscriptions on a single topic (in particular - none).

So let's create a `clicks-receiver` subscription:

```bash
curl -d '{"name": "clicks-receiver", "endpoint": "http://requestb.in/1isy54g1", "supportTeam": "my-team"}' -H "Content-Type: application/json" http://10.10.10.10:8090/topics/com.example.events.clicks/subscriptions
```

(replace `http://requestb.in/1isy54g1` with your local service url or your own RequestBin link)

List topic subscriptions:

```bash
curl http://10.10.10.10:8090/topics/com.example.events.clicks/subscriptions
["clicks-receiver"]
```

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

Congratulations! The message should be delivered to your service or visible via e.g. ``http://requestb.in/1isy54g1?inspect``.

## Stopping the system

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
