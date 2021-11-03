#!/bin/bash

BOOTSTRAP_SERVER='localhost:9092'
READINESS_CHECK_TOPIC='ready-kafka-container-cluster'
BROKER_COUNT=$1

set -e

[[ $(kafka-topics --bootstrap-server $BOOTSTRAP_SERVER --describe --topic $READINESS_CHECK_TOPIC | wc -l) > 1 ]] && kafka-topics --bootstrap-server $BOOTSTRAP_SERVER --delete --topic $READINESS_CHECK_TOPIC

kafka-topics --bootstrap-server $BOOTSTRAP_SERVER \
             --topic $READINESS_CHECK_TOPIC \
             --create \
             --partitions $BROKER_COUNT \
             --replication-factor $BROKER_COUNT \
             --config min.insync.replicas=$BROKER_COUNT

MESSAGE="`date -u`"
echo "$MESSAGE" | kafka-console-producer --broker-list $BOOTSTRAP_SERVER --topic $READINESS_CHECK_TOPIC --producer-property acks=all

kafka-console-consumer --bootstrap-server $BOOTSTRAP_SERVER --topic $READINESS_CHECK_TOPIC --from-beginning --timeout-ms 2000 --max-messages 1 | grep "$MESSAGE"
kafka-topics --bootstrap-server $BOOTSTRAP_SERVER --delete --topic $READINESS_CHECK_TOPIC
