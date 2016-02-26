#!/usr/bin/env bash

add-apt-repository ppa:openjdk-r/ppa 2>/dev/null
apt-get -y update
apt-get -qq install curl zip unzip jq git openjdk-8-jdk python-pip

source /vagrant/vagrant_provisioning/install_supervisord.sh
source /vagrant/vagrant_provisioning/install_kafka.sh
source /vagrant/vagrant_provisioning/install_puppet_modules.sh
