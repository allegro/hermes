## 0.10.5 (31.01.2017)

### Enhancements

#### ([688](https://github.com/allegro/hermes/pull/688)) Selective algorithm healing

Improved durability of assignments during restarts and zookeeper flaps.
Reporting of assignments and running consumers has been improved and made consistent.
More reliable handling of consumer processes.

##### Migration guide:

To utilize these improvements it is required to stop all instances in hermes cluster, remove all nodes from `{zookeeper.root}/consumers-workload/{kafka.cluster.name}/runtime` and restart instances.

This adds a marker in selective algorithm's consumer assignments, which allows rebalancing with removing automatically created assignments.

Alternatively, to avoid switching off your cluster, a script updating assignments' zookeeper nodes' data to `AUTO_ASSIGNED` can be used. It should be also applied after all nodes run the new version, as previous run could shuffle assignments during deployment.

#### ([698](https://github.com/allegro/hermes/pull/698)) Fix Dockerfile build

## 0.10.4 (23.01.2017)

### Enhancements

#### ([690](https://github.com/allegro/hermes/pull/690)) Update json-avro-converter to 0.2.5
#### ([687](https://github.com/allegro/hermes/pull/687)) Added throughput metric
#### ([684](https://github.com/allegro/hermes/pull/684)) Limit number of retries for inflight on Frontend graceful shutdown

### Bugfixes

#### ([694](https://github.com/allegro/hermes/issues/694)) Leaking file descriptors

Handling corner case in a race between ack and timeout task. 

Because of it number of messages in backup storage was growing with a time.
Eventually, it lead to full backup-storage and further writes ended with an exception which was not caught.
This exception was the reason of file descriptor leak.

Mentioned corner case was fixed in this issue. Beside that, additional exception handling was added and backup-storage size
is from now on monitored.

#### ([695](https://github.com/allegro/hermes/pull/695)) Decouple filtering rate limiting from backpressure based rate limiting

Filtered messages do not influence on a sending rate.

#### ([686](https://github.com/allegro/hermes/pull/686)) Do not merge topic.contentType with default value

Fix in hermes-console.

## 0.10.3 (02.01.2017)

### Bugfixes

#### ([675](https://github.com/allegro/hermes/pull/675)) Audit of subscription status changes

#### ([674](https://github.com/allegro/hermes/pull/674)) Validate topic before saving

#### ([679](https://github.com/allegro/hermes/issues/679)) hermes-client handles sender errors

#### ([676](https://github.com/allegro/hermes/pull/676)) Fix saving changes in topic maxMessageSize attribute

## 0.10.2 (19.12.2016)

This release introduces a crucial warming-up phase when starting Hermes Frontend.

### Features

#### ([591](https://github.com/allegro/hermes/issues/591)) Frontend graceful startup

Frontend tries to load and cache all Avro schemas and Kafka topic metadata before accepting any traffic.
Before this change large clusters were throwing 5xx and had very big latencies during warmup phase.
Currently startup moment is barely noticable for clients (and in metrics).

#### ([667](https://github.com/allegro/hermes/pull/667)) Declare max message size on topic

With this change users are asked to specify the maximum size of message on a topic during topic creation.
This size is then used to calculate the size of Kafka buffers in Hermes Consumers. Prior to this change
Consumer Kafka buffers were set to the same size for every topic (default: 10Mb per partition), which could
cause crashes when starting Consumers with large number of subscriptions with lags.

By default message size is a soft limit, `warn` log is emitted when message larger than declared size is received.
`frontend.force.topic.max.message.size` flag can be switched to make it a hard limit (Frontend will return 
http `413 Payload Too Large` status).

Also calculation based on message size os disabled by default (will be enabled by default in next versions). To use this
feature set `consumer.use.topic.message.size` flag.

#### ([666](https://github.com/allegro/hermes/pull/666)) Options to configure Consumer HTTP client SSL Context

New options to configure Consumers HTTP client:

* `consumer.http.client.validate.certs`
* `consumer.http.client.validate.peer.certs`
* `consumer.http.client.enable.crldp`

#### ([665](https://github.com/allegro/hermes/pull/665)) Allow to specify allowed topic content types in Hermes Console

### Bugfixes

#### ([663](https://github.com/allegro/hermes/pull/663)) Fetch -2min of data from Graphite and take first non-empty value

#### ([664](https://github.com/allegro/hermes/pull/663)) Use proper type of metrics in Consumer workload metrics

#### ([652](https://github.com/allegro/hermes/issues/652)) Proper configuration for Zookeeper retries

## 0.10.1 (29.11.2016)

This is a bugfix release improving `schema-registry` integration and retransmission on large clusters.

### Bugfixes

- **([#630](https://github.com/allegro/hermes/pull/630)) Retransmission is unstable**
- **([#640](https://github.com/allegro/hermes/pull/640)) Schema cache reload 404-proof**

## 0.10.0 (22.11.2016)

This release introduces a lot of performance optimizations related to publishing messages to Hermes.

### Features

#### ([#518](https://github.com/allegro/hermes/pull/518)) Frontend performance

- implemented hermes-benchmarks module with frontend benchmark tests written in jmh
- servlet layer was removed, publishing is done on raw undertow handlers
- timeouts mechanism (202, 408) was redesigned, locks were elminated
- sped up metrics invocation during message publishing, from now on they are kept in topics cache

#### ([#559](https://github.com/allegro/hermes/issues/559)) Topic ban button

Thanks to topic ban button events published on a topic can be cheaply discarded.
This feature can be used when some misbehaving publisher is detected,
i.e. starts to push enormous events or all his events have invalid schema.

### Bugfixes

#### ([#636](https://github.com/allegro/hermes/issues/636)) ConsumersProcessSupervisor is not killing any consumer process 

## 0.9.3 (16.11.2016)

### Bugfixes

- **([#626](https://github.com/allegro/hermes/pull/626)) Custom KafkaNamesMapper can be used too late**
- **([#628](https://github.com/allegro/hermes/pull/628)) Hermes should operates on "Schema-Version" header instead of "Hermes-Schema-Version"**
- **([#630](https://github.com/allegro/hermes/pull/630)) Retransmission is unstable**

## 0.9.2 (15.11.2016)

### Features

#### ([#612](https://github.com/allegro/hermes/pull/612)) Added explicit CORS allowed domain configuration option

### Enhancements

#### ([#619](https://github.com/allegro/hermes/pull/619)) Updated kafka-producer configuration

In the current version of kafka-producer (0.10.1) `request.timeout.ms` parameter is also used as a timeout for dropping batches from internal accumulator. 
Therefore, it is better to increase this timeout to very high value, because when kafka is unreachable we don't want to drop messages but buffer them in accumulator until is full.
This behavior will change in future version of kafka-producer.

More information on this issue can be found in
[kafka-users group archives](http://mail-archives.apache.org/mod_mbox/kafka-users/201611.mbox/%3C81613078-5734-46FD-82E2-140280758BC6@gmail.com%3E)

### Bugfixes

- **([#614](https://github.com/allegro/hermes/pull/614)) JSON-to-Avro dry run fix for Hermes-incompatible schemas** 
- **([#621](https://github.com/allegro/hermes/pull/621)) Schema-related frontend HTTP responses fix**
- **([#622](https://github.com/allegro/hermes/pull/622)) Fixing occasional null pointer when reading consumer assignments**
- **([#624](https://github.com/allegro/hermes/pull/624)) Catching unchecked exceptions in schema-versions cache that previously weren't logged**
- **([#616](https://github.com/allegro/hermes/pull/616)) Fixing bug with sync commit after each filtered message**


## 0.9.1 (02.11.2016)

This patch version was released mostly because of **Schema version cache fix [#608](https://github.com/allegro/hermes/issues/608)**

Beside that:
 - documentation about schema repository was updated
 - integration tests should be more reliable

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

Before upgrading, make sure that offsets are committed and stored in Kafka (option: `kafka.consumer.dual.commit.enabled` is set to `true` or
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
