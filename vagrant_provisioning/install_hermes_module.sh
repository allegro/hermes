#!/usr/bin/env bash

module="$1"

echo "Building Hermes module [$module]..."

/vagrant/gradlew -q -p /vagrant/hermes-${module} distZip -Pdistribution

echo -n "Stopping module [$module]..."
supervisorctl stop hermes-${module} || true

echo "Installing module [$module]..."

rm -rf /opt/hermes-${module}*
unzip -q /vagrant/hermes-${module}/build/distributions/hermes-${module}-*.zip -d /opt/
mv /opt/hermes-${module}-* /opt/hermes-${module}

echo -n "Starting module [$module]..."
supervisorctl start hermes-${module}