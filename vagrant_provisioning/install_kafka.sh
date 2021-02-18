#!/usr/bin/env bash

KAFKA_VERSION=2.2.0
SCALA_VERSION=2.11

if [ ! -d /opt/kafka ]; then
    echo "Installing Apache Kafka ${KAFKA_VERSION}"
    distUrls=(
      "$(curl -sS https://www.apache.org/dyn/closer.cgi\?as_json\=1 | jq -r '.preferred')"
      "https://archive.apache.org/dist/"
    )

    for distUrl in "${distUrls[@]}"; do
      url="${distUrl}kafka/${KAFKA_VERSION}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz"
      if curl -sS ${url} --output /tmp/kafka.tgz --fail; then
        echo "Downloaded Apache Kafka from: $url"
        break;
      fi
    done

    tar xzf /tmp/kafka.tgz -C /opt
    rm /tmp/kafka.tgz
    mv /opt/kafka_${SCALA_VERSION}-${KAFKA_VERSION} /opt/kafka
else
    echo "Apache Kafka ${KAFKA_VERSION} already installed"
fi
