#!/usr/bin/env bash

add-apt-repository ppa:webupd8team/java
apt-get -y update
apt-get -qq install curl unzip jq

echo "Installing Java 1.8"
echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
apt-get install -qq oracle-java8-set-default

source /vagrant/vagrant_provisioning/install_kafka.sh
source /vagrant/vagrant_provisioning/install_hermes.sh
source /vagrant/vagrant_provisioning/install_supervisord.sh
