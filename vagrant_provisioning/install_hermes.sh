#!/usr/bin/env bash

echo "Building Hermes..."

/vagrant/gradlew -q -p /vagrant clean distZip -Pdistribution

echo "Installing Hermes..."

modules=( frontend consumers management )

for module in "${modules[@]}"; do
    mv /vagrant/hermes-$module/build/distributions/hermes-$module-*.zip /tmp \
        && unzip -q /tmp/hermes-${module}-* -d /opt \
        && rm /tmp/hermes-${module}-* \
        && mv /opt/hermes-${module}-* /opt/hermes-${module}
done
