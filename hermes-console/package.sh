#!/bin/bash

NAME=hermes-console

NODE_VERSION="v5.1.0"
NODE_DIST="node-$NODE_VERSION-linux-x64"

printf "Packaging Hermes Console\n"

if [ ! -e dist ]; then
    mkdir -p dist
fi

if [ ! -e dist/$NODE_DIST.tar.gz ]; then
    printf "Downloading NodeJS distribution version $NODE_VERSION\n"
    wget --quiet --no-clobber "https://nodejs.org/dist/$NODE_VERSION/$NODE_DIST.tar.gz" --directory-prefix dist
    mkdir -p dist/node
    tar --extract --keep-old-files --strip 1 --file dist/$NODE_DIST.tar.gz -C dist/node
fi

export PATH=$(pwd)/dist/node/bin:$PATH

printf "Running NPM and bower\n"

npm install --production --yes
node_modules/.bin/bower install --allow-root

printf "Creating package: dist/hermes-console.zip\n"

# first step - create base archive
zip --quiet --symlinks --recurse-paths dist/hermes-console.zip node_modules package.json serve.js static run.sh

# second step - add node distribution to archive root
(cd dist && zip --quiet --symlinks --recurse-paths hermes-console.zip node)