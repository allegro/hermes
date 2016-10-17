## 0.9.0 (17.10.2016)

This release introduces Kafka 0.10 producer/consumer API and is no longer compatible with Kafka 0.8.x and 0.9.x deployments.

### Features

#### ([#558](https://github.com/allegro/hermes/issues/558)) Use Kafka 0.10 producer/consumer API

**This change breaks backwards compatibility - Hermes will not run on 0.8.x, 0.9.x Kafka clusters**

Hermes uses Kafka 0.10 APIs. The change is not big for producers in Frontend module, but it rearranged whole Consumers module.

The benefits of moving to Kafka 0.10 (except from leaving the deprecated APIs behind) are:

* decreased number of active threads: in cluster with ~600 subscriptions number of threads decreased from ~4400 to ~700
* decreased memory consumption: same cluster, memory usage dropped by 10-20%
* decreased CPU consumption: same cluster, day-to-day CPU consumption dropped by ~10%
* greatly decreased shutdown time

The change is transparent for the end users.

**Upgrading note**

Before upgraing, make sure that offsets are committed and stored in Kafka (option: `kafka.consumer.dual.commit.enabled` is set to `true` or 
`kafka.consumer.offsets.storage` is set to `kafka` (default) in Consumers module).

When upgrading, all Consumers should be stopped at once and started with new version.

#### ([593](https://github.com/allegro/hermes/pull/593)) Confluent Schema Registry integration

**Breaking change: Support for storing and validating JSON schemas has been removed**

Hermes be integrated with [Confluent Schema Registry](https://github.com/confluentinc/schema-registry) to store and read Avro schemas. We kept existing integration with [schemarepo.org](http://schemarepo.org) repository. To switch between implementations, use `schema.repository.type` option:

* `schema_repo` for "old" schemarepo.org
* `schema_registry` for Confluent Schema Registry

### Enhancements

#### ([#592](https://github.com/allegro/hermes/pull/592)) Management: Update Spring Boot (1.4.1) and Jersey (2.23)

#### ([#595](https://github.com/allegro/hermes/pull/595)) Update tech.allegro.schema.json2avro to 0.2.4

## 0.8.12 (23.09.2016)

### Features

#### ([#566](https://github.com/allegro/hermes/issues/566)) Auditing management operations

All operations in Management can be auditable. By default this option is disabled, but can be enabled using:

```
audit.enabled = true
```

By default changes are sent to logs, but own implementation can be provided. Reed more in [auditing documentation](http://hermes-pubsub.readthedocs.io/en/latest/configuration/security/#management-operations-auditing).

#### ([#481](https://github.com/allegro/hermes/issues/481)) Delay between retries in Hermes Client

It is now possible to specify delay between consecutive retries of sending message.

```java
HermesClient client = HermesClientBuilder.hermesClient(...)
    .withRetries(3)
    .withRetrySleep(100, 10_000)
```

The delay can rise exponentially in specified range (100ms to 10 seconds in example above).

### Bugs

#### ([577](https://github.com/allegro/hermes/issues/557)) Consumer won't stop if there are messages in send queue

#### ([579](https://github.com/allegro/hermes/pull/579)) Wrong path to lag stats in Hermes Console

## 0.8.11 (24.08.2016)

### Features

#### ([#359](https://github.com/allegro/hermes/issues/359)) OAuth2 support [incubating]

Hermes supports Resource Owner Password Credential Grant scenario. It is possible to declare multiple OAuth providers
in Hermes, along with their credentials. Each subscription can choose a provider and defines own user & password.

### Enhancements

#### ([#556](https://github.com/allegro/hermes/pull/556)) Added source and target hostname information to tracking

Tracking information now contains additional fields: `hostname` and `remote_hostname`, which are:

* on Frontend side:
    * `hostname`: hostname of Frontend host that received the message
    * `remote_hostname`: IP address of events producer (who published)
* on Consumers side:
    * `hostname`: hostname of Consumer host that was handling the message
    * `remote_hostname`: IP address/hostname of host that acknowledged/rejected message (who received)

#### ([#561](https://github.com/allegro/hermes/pull/561)) Consumers process model improvements

Improving the stability of new internal Consumers process model by adding consumer process graceful shutdown
and filtering unwatned signals (i.e. sequential START & STOP) which might cause instability.

For monitoring purposes two new metrics (counters) were created in Consumers that compare the assignments state vs 
the actual consumers running:

* `consumers-workload.monitor.missing.count` - how many processes are missing compared to assigned amount
* `consumers-workload.monitor.oversubscribed.count` - how many processes exist although they should not, as this
    instance of Consumers is not assigned to run them

In addition to metrics, warning logs are emitted with details about subscription names missing/oversubscribed.

#### ([#563](https://github.com/allegro/hermes/pull/563)) Apache Curator 2.11.0 and Guava 19.0
