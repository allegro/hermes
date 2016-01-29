#!/bin/bash

export PATH=$(pwd)/node/bin:$PATH

node serve.js "$@"