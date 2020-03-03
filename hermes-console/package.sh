#!/bin/bash

NAME=hermes-console

if [ -z ${1+x} ]; then
    ARCHIVE_NAME=hermes-console
else
    ARCHIVE_NAME=$1
fi

NODE_VERSION="v6.11.4"
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
node_modules/.bin/bower install --allow-root -F

printf "Creating directory: dist/static\n"

# copy static contents
cp -r static dist
