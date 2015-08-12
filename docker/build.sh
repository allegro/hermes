#!/bin/bash

rm -rf build
mkdir -p build
cd ..
version=`./gradlew cV -q | grep version | sed -n 's/Project version: \(.*\)/\1/p'`
./gradlew -q clean distZip -Pdistribution && cd -

modules=( frontend consumers management )

for module in "${modules[@]}"; do
  cp ../hermes-$module/build/distributions/hermes-$module-*.zip ./build/
  docker build -f Dockerfile-$module -t allegro/hermes-$module:$version .
done

rm -rf build
