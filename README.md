Hermes
======

[![Build Status](https://github.com/allegro/hermes/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/allegro/hermes/actions/workflows/ci.yml?query=branch%3Amaster)
[![Documentation Status](https://readthedocs.org/projects/hermes-pubsub/badge/?version=latest)](https://readthedocs.org/projects/hermes-pubsub/?badge=latest)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/pl.allegro.tech.hermes/hermes-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/pl.allegro.tech.hermes/hermes-client)

Hermes is an asynchronous message broker built on top of [Kafka](http://kafka.apache.org/).
We provide reliable, fault tolerant REST interface for message publishing and adaptive push
mechanisms for message sending.

See our 10-minute getting started guide with Docker: [Getting started](http://hermes-pubsub.readthedocs.org/en/latest/quickstart/)

Visit our page: [hermes.allegro.tech](http://hermes.allegro.tech)

See our full documentation: [http://hermes-pubsub.readthedocs.org/en/latest/](http://hermes-pubsub.readthedocs.org/en/latest/)

If you have any question or idea regarding the project, please feel free to reach us out using [GitHub discussions](https://github.com/allegro/hermes/discussions).

## License

**hermes** is published under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Development 

### Code formatting
For code formatting we use [google-java-format](https://github.com/google/google-java-format/tree/master).
Following steps are required for optimal dev experience in IJ:

1. Download [google-java-format plugin](https://plugins.jetbrains.com/plugin/8527-google-java-format)
2. [Set custom VM options required for IJ plugin](https://github.com/google/google-java-format/tree/master?tab=readme-ov-file#intellij-jre-config)
3. Go to `Settings > google-java-format` and click `Enable google java-format` (should be checked by default)
4. Go to `Settings > Tools > Actions on Save` and enable `Reformat code` and `Optimize imports` for Java files

Each save should automatically trigger reformat.

If you want to debug the CLI check on macOS:

```shell
wget https://github.com/google/google-java-format/releases/download/v1.23.0/google-java-format_darwin-arm64
chmod a+x google-java-format_darwin-arm64
chmod a+x .github/scripts/check-google-java-format.sh
./.github/scripts/check-google-java-format.sh                                                                                                                     
```

or if you are on Linux:

```shell
wget https://github.com/google/google-java-format/releases/download/v1.23.0/google-java-format_linux-x86-64
chmod a+x google-java-format_linux-x86-64
chmod a+x .github/scripts/check-google-java-format.sh
./.github/scripts/check-google-java-format.sh        
```

You can also run the following command to fix formatting for the whole project:

```shell
./.github/scripts/check-google-java-format.sh --fix
```
