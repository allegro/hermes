#!/bin/bash

STARTER_SCRIPT=<STARTER_SCRIPT>

touch /tmp/start

while :; do
	if [ -f $STARTER_SCRIPT ]; then
		if [ -f /tmp/stop ]; then
		  rm /tmp/stop;
		  /usr/bin/kafka-server-stop;
		elif [ -f /tmp/start ]; then
		  rm /tmp/start;
		  bash -c "$STARTER_SCRIPT &";
		fi
	fi
	sleep 0.1
done
