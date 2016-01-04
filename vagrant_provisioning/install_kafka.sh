#!/usr/bin/env bash

KAFKA_VERSION=0.8.2.2
SCALA_VERSION=2.10

echo "Installing Apache Kafka ${KAFKA_VERSION}"

mirror=$(curl -sS https://www.apache.org/dyn/closer.cgi\?as_json\=1 | jq -r '.preferred')
url="${mirror}kafka/${KAFKA_VERSION}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz"
curl -sS ${url} --output /tmp/kafka.tgz

tar xzf /tmp/kafka.tgz -C /opt
rm /tmp/kafka.tgz
mv /opt/kafka_${SCALA_VERSION}-${KAFKA_VERSION} /opt/kafka
