#!/usr/bin/env bash

echo "Building Hermes Console..."

(cd /vagrant/hermes-console && ./package.sh)

unzip -o -qq /vagrant/hermes-console/dist/hermes-console.zip -d /opt/
cp -f /vagrant/vagrant_provisioning/conf/hermes-console-config.json /opt/hermes-console/config.json

IP=`ifconfig eth1 | grep "inet addr" | tr -s ' ' | awk -F'[: ]' '{print $4}'`
sed -i -e "s/PUBLIC_IP/$IP/g" /opt/hermes-console/config.json