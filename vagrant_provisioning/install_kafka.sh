#!/usr/bin/env bash

KAFKA_VERSION=2.0.0
SCALA_VERSION=2.11

if [ ! -d /opt/kafka ]; then
    echo "Installing Apache Kafka ${KAFKA_VERSION}"
    mirror=$(curl -sS https://www.apache.org/dyn/closer.cgi\?as_json\=1 | jq -r '.preferred')
    url="${mirror}kafka/${KAFKA_VERSION}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz"
    curl -sS ${url} --output /tmp/kafka.tgz

    tar xzf /tmp/kafka.tgz -C /opt
    rm /tmp/kafka.tgz
    mv /opt/kafka_${SCALA_VERSION}-${KAFKA_VERSION} /opt/kafka
else
    echo "Apache Kafka ${KAFKA_VERSION} already installed"
fi
