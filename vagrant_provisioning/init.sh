#!/usr/bin/env bash

apt-get -y update
apt-get -qq install curl zip unzip jq git openjdk-8-jdk

source /vagrant/vagrant_provisioning/install_supervisord.sh
source /vagrant/vagrant_provisioning/install_kafka.sh
