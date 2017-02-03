#!/bin/bash

PORT=${PORT:-8090}
EXTRA_OPS="$@"

echo "Starting server in $PORT"
echo "Extra config $EXTRA_OPS"

java -jar /root/wiremock-standalone-2.5.0.jar --port $PORT --print-all-network-traffic $EXTRA_OPS
