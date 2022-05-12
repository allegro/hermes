Hermes
======

[![Build Status](https://github.com/allegro/hermes/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/allegro/hermes/actions/workflows/ci.yml?query=branch%3Amaster)
[![Documentation Status](https://readthedocs.org/projects/hermes-pubsub/badge/?version=latest)](https://readthedocs.org/projects/hermes-pubsub/?badge=latest)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/pl.allegro.tech.hermes/hermes-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/pl.allegro.tech.hermes/hermes-client)
[![Join the chat](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/allegro/hermes?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Hermes is an asynchronous message broker built on top of [Kafka](http://kafka.apache.org/).
We provide reliable, fault tolerant REST interface for message publishing and adaptive push
mechanisms for message sending.

See our 10-minute getting started guide with Vagrant: [Getting started](http://hermes-pubsub.readthedocs.org/en/latest/quickstart/)

Visit our page: [hermes.allegro.tech](http://hermes.allegro.tech)

See our full documentation: [http://hermes-pubsub.readthedocs.org/en/latest/](http://hermes-pubsub.readthedocs.org/en/latest/)

Questions? We are on [gitter](https://gitter.im/allegro/hermes).

## Development

File docker/docker-compose.development.yml disables deployment of hermes frontend, management, and consumers.

We have to provide an environment (Kafka, ZooKeeper, Graphite, Schema Registry) with command executed in the project directory:

`docker-compose -f docker/docker-compose.yml -f docker/docker-compose.development.yml up`

To start hermes frontend, management and consumers we can use the following commands

`./gradlew -p hermes-frontend run`

`./gradlew -p hermes-management run`

`./gradlew -p hermes-consumers run`

or use `Run/Debug Configurations` in IntelliJ

## License

**hermes** is published under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).
