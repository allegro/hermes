#!/usr/bin/env bash

if pgrep supervisord >/dev/null 2>&1; then
    echo "Supervisord is already running..."
else
    echo "Starting supervisord..."
    supervisord -c /etc/supervisord.conf -u root
fi
