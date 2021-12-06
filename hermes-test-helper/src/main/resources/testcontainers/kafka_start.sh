#!/bin/bash

export KAFKA_ZOOKEEPER_CONNECT='<ZOOKEEPER_CONNECT>'
export KAFKA_ADVERTISED_LISTENERS='PLAINTEXT://localhost:<KAFKA_MAPPED_PORT>,BROKER://<BROKER_HOSTNAME>:9092,INTERNAL_CLIENT://<BROKER_HOSTNAME>:<KAFKA_INTERNAL_CLIENT_PORT>'

. /etc/confluent/docker/bash-config
/etc/confluent/docker/configure
/etc/confluent/docker/launch
