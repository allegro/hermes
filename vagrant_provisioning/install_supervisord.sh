#!/usr/bin/env bash

echo "Installing supervisord..."
pip install supervisor
mkdir -p /var/log/supervisor
cp /vagrant/vagrant_provisioning/conf/supervisord.conf /etc/supervisord.conf
