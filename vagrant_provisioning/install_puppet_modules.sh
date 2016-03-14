#!/usr/bin/env bash


if [ ! -d /etc/puppet/modules/graphite ]; then
    echo "Installing Puppet module Graphite..."
    puppet module install dwerder-graphite
else
    echo "Puppet module Graphite already installed..."
fi