#!/bin/bash

rm -rf build
mkdir -p build
cd .. && ./gradlew -q clean distZip && cd -

modules=( frontend consumers management )

for module in "${modules[@]}"; do
  cp ../hermes-$module/build/distributions/hermes-$module-*.zip ./build/
  docker build -f Dockerfile-$module -t allegro/hermes-$module .
done

