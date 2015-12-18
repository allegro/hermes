#!/usr/bin/env bash

echo "Installing supervisord..."
curl -sS https://bootstrap.pypa.io/get-pip.py | python
pip install supervisor
mkdir -p /var/log/supervisor
cp /vagrant/vagrant_provisioning/conf/supervisord.conf /etc/supervisord.conf
