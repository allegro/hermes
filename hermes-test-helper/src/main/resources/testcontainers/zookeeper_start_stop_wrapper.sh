#!/bin/bash

touch /tmp/start

while :; do
		if [ -f /tmp/stop ]; then
		  rm /tmp/stop;
		  zookeeper-server-stop;
		elif [ -f /tmp/start ]; then
		  rm /tmp/start;
		  bash -c "/etc/confluent/docker/run &";
		fi
	sleep 0.1
done
