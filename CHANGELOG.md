## [Unreleased]

### ...

## 1.0.4 (17.06.2019)

### Enhancements

#### ([1035](https://github.com/allegro/hermes/pull/1035)) Handling unparsable values in queries

### Bugfixes

#### ([1038](https://github.com/allegro/hermes/pull/1038)) Fix to filtering duplicated signals

## 1.0.3 (10.06.2019)

### Enhancements

#### ([1032](https://github.com/allegro/hermes/pull/1032)) Fixing MPSC queue's `drain` method to return all items

## 1.0.2 (06.06.2019)

### Enhancements

#### ([1031](https://github.com/allegro/hermes/pull/1031)) Added logging related to offsets committing
#### ([1025](https://github.com/allegro/hermes/pull/1025)) ReadOnly mode for Management

This feature allows to manually switch Hermes Management to ReadOnly mode. 
During maintenance mode this protects Hermes from any unexpected changes.
Switch is done at runtime via `/mode` admin endpoint.

### Bugfixes

#### ([1031](https://github.com/allegro/hermes/pull/1031)) Fixed monitored MPSC queue utilization meter 

## 1.0.1 (29.05.2019)

### Enhancements

#### ([1029](https://github.com/allegro/hermes/pull/1029)) Attaching Keep-Alive header to frontend responses

Added option `frontend.keep.alive.header.enabled` (default false) to enable attaching Keep-Alive header with timeout
parameter (defined by `frontend.keep.alive.header.timeout.seconds`) to Hermes Frontend responses.

### Bugfixes

#### ([1027](https://github.com/allegro/hermes/pull/1027)) Message sending handlers triggered only when consumer is running
#### ([1028](https://github.com/allegro/hermes/pull/1028)) Improved handling unavailable metrics

## 1.0.0 (16.05.2019)

After 5 years of developing and maintaining Hermes, we are very excited to announce that version 1.0
has been released. 🎊 🥳 🎂

Version 1.0 is symbolic. It doesn’t contain any changes in Hermes in comparison to the previous one.
It’s a cut-off from stable and mature code.

## 0.16.2 (24.04.2019)

### Enhancements

#### ([1019](https://github.com/allegro/hermes/pull/1019)) Extended offline clients by 'owners' field

## 0.16.1 (23.04.2019)

### Enhancements

#### ([1016](https://github.com/allegro/hermes/pull/1016)) Moved parameter max.message.bytes to global configuration

## 0.16.0 (12.04.2019)

### Enhancements

#### ([1006](https://github.com/allegro/hermes/pull/1006)) Enable setting idle timeout for jetty client
#### ([1003](https://github.com/allegro/hermes/pull/1003)) Fix apache client settings used for batch subscriptions

## 0.15.9 (29.03.2019)

### Features

#### ([996](https://github.com/allegro/hermes/pull/996)) Attaching HTTP headers with subscription identity

Added subscription flag `subscriptionIdentityHeadersEnabled`. When you set it to true, then
Hermes for each message send to subscriber will attach headers like `Hermes-Topic-Name`, `Hermes-Subscription-Name`.
Thanks to this headers subscriber can verify whether message comes from expected topic and subscription.
This feature is related to security. 

#### ([995](https://github.com/allegro/hermes/pull/995)) Consumer groups in console

Subscription view in Hermes-console received new panel called `Diagnostics`. The panel contains only single button
redirecting to consumers-group endpoint. It reveals internal information about Kafka topic, for example current offset,
lag or which consumer is attached to which partition.
These kind of information sometimes are helpful for Hermes administrator to analyze subscription. 
So for we read them manually via Kafka scripts, now we can use consumer-group endpoint which is much simple way.

The view of `Diagnostics` panel is very basic now, in the future we can improve it.

### Enhancements
 
#### ([991](https://github.com/allegro/hermes/pull/991)) Spring-boot upgrade in hermes-management

Spring-boot in hermes-management was upgraded from `1.4.1` to `2.1.3`. Thanks @alasun for contribution.

#### ([984](https://github.com/allegro/hermes/pull/984)) Extend avropath filter with array support

Thanks this enhancement we can use array selectors in avropath filters. For example we can use:

- name[5]
- name[\*], where \* is as wildcard for matching all array items 

Thanks @karolhor for contribution.

#### ([997](https://github.com/allegro/hermes/pull/997)) Additional logging for MatcherQuery
#### ([1000](https://github.com/allegro/hermes/pull/1000)) Added Apache License, Version 2.0

## 0.15.8 (11.03.2019)

### Enhancements

#### ([982](https://github.com/allegro/hermes/pull/982)) Giving names to all anonymous thread pools
#### ([986](https://github.com/allegro/hermes/pull/986)) Consumer groups description
#### ([985](https://github.com/allegro/hermes/pull/985)) Replacing no longerworking recycleb.in url with other online requests service
#### ([990](https://github.com/allegro/hermes/pull/990)) More logging on writing to consumer assignment registry

## 0.15.7 (25.01.2019)

### Enhancements

#### ([979](https://github.com/allegro/hermes/pull/979)) Hermes Mock predicates
#### ([977](https://github.com/allegro/hermes/pull/977)) Hermes Mock with ClassRule and JUnit 5 extension
#### ([972](https://github.com/allegro/hermes/pull/972)) Improved subscription health problem indicator

### Bugfixes

#### ([978](https://github.com/allegro/hermes/issues/978)) Fix stale retransmission bug
#### ([968](https://github.com/allegro/hermes/pull/968)) Subscription delivery type can be updated from batch to serial
#### ([967](https://github.com/allegro/hermes/pull/967)) Fixes version replacement pattern
#### ([974](https://github.com/allegro/hermes/pull/974)) Setting kafka consumer max.poll.interval.ms to Integer.MAX_VALUE
#### ([973](https://github.com/allegro/hermes/pull/973)) Handling deprecated owner sources

## 0.15.6 (21.01.2018)

### Enhancements

#### ([966](https://github.com/allegro/hermes/pull/966)) Adds endpoint returning description of unhealthy subscriptions
#### ([971](https://github.com/allegro/hermes/pull/971)) Use OpenJDK8 instead of OracleJDK8

### Bugfixes

#### ([976](https://github.com/allegro/hermes/pull/976)) Make HermesMock API public
#### ([970](https://github.com/allegro/hermes/pull/970)) Fixes metrics for batch and filtered subscriptions

## 0.15.5 (16.01.2018)

### Bugfixes

#### ([969](https://github.com/allegro/hermes/pull/969)) Temporary retransmission fix

## 0.15.4 (14.01.2018)

### Enhancements

#### ([963](https://github.com/allegro/hermes/pull/963)) Disabling not used graphite metrics attributes
#### ([965](https://github.com/allegro/hermes/pull/965)) Updating kafka version to 2.0.0 in Vagrant

## 0.15.3 (09.01.2018)

### Enhancements

#### ([960](https://github.com/allegro/hermes/pull/960)) Multi Elasticsearch log repository
#### ([959](https://github.com/allegro/hermes/pull/959)) Write test output in Junit format

## 0.15.2 (5.12.2018)

### Enhancements

#### ([957](https://github.com/allegro/hermes/pull/957)) Optimized elasticsearch template mapping

## 0.15.1 (28.11.2018)

### Enhancements

#### ([956](https://github.com/allegro/hermes/pull/956)) Updated elasticsearch to 6.1.4 version in hermes-tracker
#### ([955](https://github.com/allegro/hermes/pull/955)) Changed format for subscription healthcheck endpoint

From now on we can list unhealthy subscriptions via following endpoint on hermes-management:

```
POST `/unhealthy
```

We can also provide parameters:
```
POST /unhealthy?ownerSourceName={ownerSourceName}&ownerId={service_id}&respectMonitoringSeverity=true
```

Thanks the first two parameters you can narrow down search results to unhealthy subscriptions owned by provided
{ownerId} from {ownerSourceName}. 

The last flag decides whether monitoring severity flag on subscription should be respected. If true then only
unhealthy subscriptions with severity monitor set to `Important` or `Critical` will be returned by unhealthy endpoint.

## 0.15.0 (06.11.2018)

### Enhancements

8 changes were merged in this release which were done during Allegro Hacktoberfest event.

#### ([919](https://github.com/allegro/hermes/pull/919)) Test case for lowercase header by @adididas122
#### ([935](https://github.com/allegro/hermes/pull/935)) Added log ready debug message to hermes response interface by @mictyd 
#### ([937](https://github.com/allegro/hermes/pull/937)) 503 response without Retry-After does not ignore rate limiting by @spooz
#### ([938](https://github.com/allegro/hermes/pull/938)) Introduce randomTopic test helper method creating topic with random  name by @Theer108
#### ([940](https://github.com/allegro/hermes/pull/940)) Added message filter type validation against topic by @klacia
#### ([942](https://github.com/allegro/hermes/pull/942)) Passing max message size to kafka by @mictyd
#### ([943](https://github.com/allegro/hermes/pull/943)) Added content type validation disabling AVRO for BATCH delivery mode by @pwolaq
#### ([949](https://github.com/allegro/hermes/pull/949)) Added clone action for topics and subscriptions by @pwolaq

## 0.14.0 (25.10.2018)

### Enhancements

#### ([914](https://github.com/allegro/hermes/pull/914)) Upgrading kafka to version 2.0

This version of Hermes is safe and backward compatible, so no additional actions are required to be performed on kafka brokers.

#### ([944](https://github.com/allegro/hermes/pull/944)) Updating info about simplified release flow

## 0.13.5 (22.10.2018)

### Bugfixes

#### ([947](https://github.com/allegro/hermes/pull/947)) Fix subscription page

## 0.13.4 (22.10.2018)

### Bugfixes

#### ([933](https://github.com/allegro/hermes/pull/933)) Fix trackingmode mapping

Fix improper mapping for tracking mode in subsription edit page.

## 0.13.3 (18.10.2018)

### Features

#### ([918](https://github.com/allegro/hermes/pull/918)) Trace only discarded messages

Added options to trace only discarded messages for subscription and error for topic.

### Enhancements

#### ([916](https://github.com/allegro/hermes/pull/916)) Subscription filter on UI

Now we support editing filters via UI. Filters can be added/edited during creation or editing a subscription.

#### ([923](https://github.com/allegro/hermes/pull/923)) Avro filter supports null value

Now using avro filter we are able to filter fields with a null value.

#### ([924](https://github.com/allegro/hermes/pull/924)) Adding health status endpoint to hermes-management

#### ([925](https://github.com/allegro/hermes/pull/925)) Updating wiremock and switching it to standalone version

Updating wiremock to the latest version (2.19.0) and using standalone version where it is possible to avoid conflicts of its dependencies.

## 0.13.2 (20.09.2018)

### Enhancements

#### ([909](https://github.com/allegro/hermes/pull/909)) Schema update improvements

From now on when topic schema is updated via hermes-console then all Hermes instances are notified to load latest schema
from schema-registry as soon as possible (by default they should be notified in 2 minutes).

#### ([906](https://github.com/allegro/hermes/pull/906)) Docs for adding subscription's filters

### Bugfixes

#### ([872](https://github.com/allegro/hermes/pull/872)) Fix for reading Graphite stats in Management

## 0.13.1 (30.08.2018)

Small fix in config.properties.

Property `messages.local.buffered.storage.size.bytes` from 0.13.0 now becomes
`frontend.messages.local.buffered.storage.size.bytes`.

## 0.13.0 (28.08.2018)

All issues and pull requests: [0.13.0 milestone](https://github.com/allegro/hermes/milestone/48)

### Features

#### ([899](https://github.com/allegro/hermes/pull/899)) ChronicleMap v3

Starting from this version Hermes will use [ChronicleMap v3](https://github.com/OpenHFT/Chronicle-Map) as a temporary
buffer for messages (before that Hermes was using ChronicleMap v2).

Now there are 2 new config properties:
- `frontend.messages.local.buffered.storage.size.bytes` - describes default size for a delayed messages queue in bytes
in internal Kafka Producer Queue and Hermes Frontend Buffer. 
  
- `frontend.messages.local.storage.average.message.size.in.bytes` - describes average message size for better performance
for delayed messages in Hermes Frontend Buffer.

And also `kafka.producer.buffer.memory` was removed from a config, now `frontend.messages.local.buffered.storage.size.bytes`
is responsible for that parameter.


#### ([898](https://github.com/allegro/hermes/pull/898)) Sending Delay is not required in batch subscription

#### ([896](https://github.com/allegro/hermes/pull/896)) Make BackupMessage serializable

#### ([900](https://github.com/allegro/hermes/pull/900)) Hermes Mock documentation  

### Bugfixes

#### ([897](https://github.com/allegro/hermes/pull/897)) Fix label from `seconds` to `milliseconds`
In Hermes console there were an inconsistency regarding `requestTimeout` and `sendingDelay` labels. Label stated that
those values are in seconds, but they are in milliseconds. 

## 0.12.10 (14.08.2018)

All issues and pull requests: [0.12.10 milestone](https://github.com/allegro/hermes/milestone/47)

### Features

#### ([894](https://github.com/allegro/hermes/pull/894)) Sending delay

Sending delay feature. We want to give users possibility to postpone sending an event for given time (max 5 seconds) 
so if there are multiple topics that sends messages at the same time, then can increase chance of receiving an event 
from one topic before an event from another topic.

#### ([894](https://github.com/allegro/hermes/pull/871)) Improved processes signals management

Improve processes management to be more predictable and easy to understand.

## 0.12.9 (12.07.2018)

All issues and pull requests: [0.12.9 milestone](https://github.com/allegro/hermes/milestone/46)

### Features

#### ([886](https://github.com/allegro/hermes/pull/886)) Listing topics by their owner 

Endpoint in hermes-management to list topics by their owner with lower latency than using QueryEndpoint.

#### ([888](https://github.com/allegro/hermes/pull/888)) Listing subscriptions by their owner

Endpoint in hermes-management to list subscriptions by their owner with lower latency than using QueryEndpoint.

#### ([887](https://github.com/allegro/hermes/pull/887)) Waiting between unsuccessful polls to reduce cpu utilization

In a previous releases subscribing to topics with low rps is very cpu intensive because when polling KafkaConsumer
implementation is constantly looping until timeout is reached. We introduced simple exponentially growing strategy
to wait between unsuccessful polls which reduces cpu utilization by a significant margin in those cases.

## 0.12.8 (29.06.2018)

All issues and pull requests: [0.12.8 milestone](https://github.com/allegro/hermes/milestone/45)

## 0.12.7 (21.06.2018)

All issues and pull requests: [0.12.7 milestone](https://github.com/allegro/hermes/milestone/44)

## 0.12.5 (01.06.2018)

All issues and pull requests: [0.12.5 milestone](https://github.com/allegro/hermes/milestone/42)

## 0.12.4 (17.04.2018)

All issues and pull requests: [0.12.4 milestone](https://github.com/allegro/hermes/milestone/41)

## 0.12.3 (11.01.2018)

All issues and pull requests: [0.12.3 milestone](https://github.com/allegro/hermes/milestone/40)

## 0.12.2 (25.10.2017)

All issues and pull requests: [0.12.2 milestone](https://github.com/allegro/hermes/milestone/39)

### Features

#### ([814](https://github.com/allegro/hermes/pull/814)) Offline storage metadata

Add new metadata to topic entity. From now on it is possible to specify if data from the topic
should be persisted into any kind of offline store (like HDFS). Metadata is not used by Hermes,
but is part of the API and can be consumed by tools like [Gobblin](https://github.com/apache/incubator-gobblin)
to choose which data should be moved to HDFS and for how long should it be kept.

#### ([821](https://github.com/allegro/hermes/issues/821)) Avro message preview in human-readable form

[Topic message preview](http://hermes-pubsub.readthedocs.io/en/latest/user/topic-preview/) for Avro messages
now shows data transformed to JSON instead of raw Avro bytes.

Contributed by @mictyd

#### ([822](https://github.com/allegro/hermes/issues/822)) Detailed message when Avro validation fails

`400 Bad Message` status now returns much more meaningful information when Avro validation fails.

Contributed by @janisz.

#### ([824](https://github.com/allegro/hermes/pull/824)) Bum dependencies versions

Upgraded the following dependencies:

* Metrics to 3.2.5 
* Guava to 23.0
* Apache Curator to 2.12.0 (forced by Guava upgrade)

#### ([756](https://github.com/allegro/hermes/issues/756)) Display owner source in Console

Console now displays the source of topic and subscription owner next to the owner name.

### Bugfixes

#### ([769](https://github.com/allegro/hermes/issues/769)) Deleted topics come back to life

Fixed by upgrading Kafka client to 0.10.1.0.

#### ([812](https://github.com/allegro/hermes/pull/812)) Fixed Elasticsearch trace repo bug introduced in 0.12.0

#### ([809](https://github.com/allegro/hermes/pull/809)) Fixed Hermes Console retransmit button

Contributed by @piorkowskiprzemyslaw.

#### ([834](https://github.com/allegro/hermes/issues/834)) Max-rate Zookeeper structure cleanup script

Max-rate Zookeeper structure is not cleaned up when subscription is deleted. This, in time, leads to building
up a huge structure with lots of watches. We observed that due to the amount of watches, Consumers startup time
degrades significantly (up to 10 minutes). There is no fix for the lack of cleanup, but we created the script
that can be run once in a while to keep the structure in desired size.

## 0.12.1 (04.08.2017)

All issues and pull requests: [0.12.1 milestone](https://github.com/allegro/hermes/milestone/37)

### Features

#### ([799](https://github.com/allegro/hermes/pull/799)) Offline clients

Add new endpoint and interface in Management which can be implemented to show
if data produced on given topic has been accessed recently in any offline storage
(like Hadoop). Read more in [docs](http://hermes-pubsub.readthedocs.io/en/latest/configuration/offline-clients/).

#### ([692](https://github.com/allegro/hermes/issues/692)) Improved schema management

Avro schemas are now created/deleted in the same transaction as topic creation/deletion,
meaning that no topic is created if schema validation fails and vice versa.

Also new SchemaRepository interface method has been added which allows on validating
schema before trying to send it to Schema Registry (fail fast).

#### ([794](https://github.com/allegro/hermes/pull/794)) Query topics by metrics

### Bugfixes

#### ([758](https://github.com/allegro/hermes/issues/758)) Use timestamp in seconds in ElasticSearch message tracking

Added new field in ElasticSearch message trace object which is used to order
messages in time. This should significantly increase the speed of fetching
data from ES.

#### ([757](https://github.com/allegro/hermes/issues/757)) Rate limit schema registry calls in Consumers

There was no rate limit when trying to get schema from Schema Registry when
no cached schema matched the message. This could cause DoS attack on Schema Registry
for subscriptions with high traffic of malformed events.

#### ([777](https://github.com/allegro/hermes/issues/777)) Subscription URI constraint fix

Fixed subscription URI constraints to match URI spec.

#### ([787](https://github.com/allegro/hermes/issues/787)) Subscription validation fix

Subscriptions were validated at the wrong moment, which in some cases could lead
to ugly NullPointerException instead of validation message.

#### ([780](https://github.com/allegro/hermes/issues/780)) Metrics block topic removal

## 0.12.0 (23.06.2017)

### Features

#### ([760](https://github.com/allegro/hermes/pull/760)) Added http2 client to consumers

#### ([770](https://github.com/allegro/hermes/pull/770)) Added feature to restrict subscribing for particular topic

### Enhancements

#### ([775](https://github.com/allegro/hermes/pull/775)) Console notification box UX improvements

Notification box will resize according to body text, also error messages require user interaction in order to disappear.

#### ([778](https://github.com/allegro/hermes/pull/778)) Creating consumer signals chains

From now on, consumers logs contain information about signals like their id or type.
This simplifies analysis of consumers history.

#### ([781](https://github.com/allegro/hermes/pull/781)) Schema version aware deserialization is backward compatible

Which means that flag `schemaVersionAwareSerializationEnabled` can be set to `true` on the fly.
When the flag is enabled on a topic then consumed payload without schema version will be deserialized as well - 
   Hermes will try hard to adjust avro schema starting with the latest version.

### Bugfixes 

#### ([774](https://github.com/allegro/hermes/pull/774)) Ensuring that consumer process exists for processed signal

Fixes NullPointerException which occurred when some signals (e.x. `COMMIT`) were processed for non existing consumer.

## 0.11.4 (15.05.2017)

### Features

#### ([764](https://github.com/allegro/hermes/pull/764)) Custom button on UI.

Allows to configure custom view near topic buttons area on hermes-console. Custom view can be set via configuration file, example:

```
{
    "topic": {
        "buttonsExtension": "<a class=\"btn btn-info {{topic.contentType === 'JSON' ? 'ng-show' : 'ng-hide'}}\" ng-href='http://migrator.example/topics/{{topic.name}}'>Migrate to AVRO</a>",
    }
}
```

## 0.11.3 (13.04.2017)

### Features

#### ([753](https://github.com/allegro/hermes/issues/753)) Filtering by headers

Added new filter type: `header` that allows on filtering messages by HTTP headers. Example of filter definition:

```
{"type": "header", "header": "My-Propagated-Header", "matcher": "^abc.*"}
```

Mind that by default no headers are propagated from Frontend to Consumers. To enable headers propagation, define and register 
own `HeadersPropagator` via `HermesFrontend.Builder#withHeadersPropagator`.

#### ([749](https://github.com/allegro/hermes/pull/749)) Handling `avro/json` content type

When converting messages from JSON to Avro Hermes uses [json-avro-converter](https://github.com/allegro/json-avro-converter)
to provide smooth experience, that does not require changing already produced JSONs (for instance to to support optional 
fields).

However in some rare cases it might be desired to send JSON messages that are compatible with
[standard Avro JSON encoding](https://avro.apache.org/docs/1.8.1/spec.html#json_encoding). To use vanilla JSON -> Avro converter and bypass `json-avro-converter`, send requests with `avro/json` content type.

### Enhancements

#### ([748](https://github.com/allegro/hermes/pull/748)) Topic authorization controls and status in Console

Console now has support for toggling auth on topics. This is an opt-in feature, enable by specifying:

```
{
    "topic": {
        "authEnabled": true,
    }
}
```

In Console `config.json`.

#### ([710](https://github.com/allegro/hermes/issues/710)) Limit size of messages in preview

#### ([751](https://github.com/allegro/hermes/pull/751)) Move Zookeeper cache update logs to DEBUG level

#### ([750](https://github.com/allegro/hermes/pull/750)) Move Schema Registry cache refresh logs to DEBUG level

### Bugfixes

#### ([737](https://github.com/allegro/hermes/issues/737)) Updating subscription in hermes-console resets OAuth password

#### ([734](https://github.com/allegro/hermes/issues/734)) Prevent manual setting of subscription state to PENDING

#### ([743](https://github.com/allegro/hermes/pull/743)) Better defaults for max-rate algorithm

## 0.11.2 (15.03.2017)

### Enhancements

#### ([738](https://github.com/allegro/hermes/pull/738)) Payload content-type check and error handling

From now on, clients who send HTTP request without specified `Content-Type` on an Avro topic will receive proper error message.

#### ([739](https://github.com/allegro/hermes/pull/739)) Added latency metrics for schema registry

Metrics are available in the following path:

```
schema.<schema-repo-type>.latency.read-schema
```

### Bugfixes

#### ([740](https://github.com/allegro/hermes/issues/740)) Invalid metrics names in zookeeper for topics with underscore in name

## 0.11.1 (7.03.2017)

### Features

#### ([733](https://github.com/allegro/hermes/pull/733)) Topic authorization 

Added feature to control which system has permission to publish on particular topic.

#### ([722](https://github.com/allegro/hermes/pull/722)) Frontend security context initialization

Added feature to configure SSL context.

#### ([735](https://github.com/allegro/hermes/pull/735)) Configurable http keep_alive
#### ([707](https://github.com/allegro/hermes/pull/707)) Throughput limit

Added feature to limit throughput in bytes/sec when publishing to particular topic. 
Can be configured to work as simple threshold or dynamically calculated value.

#### ([687](https://github.com/allegro/hermes/pull/687)) Throughput metric

Metric can be found under {producer|consumer}.{hostname}.throughput.{group}.{topic}

## 0.11.0 (15.02.2017)

### Features

#### ([693](https://github.com/allegro/hermes/pull/693)) Owners instead of support teams

Replaces group and subscription support team, contact and technical owner with a 
single notion – owner. Topics and subscriptions have assigned owners, groups no longer
do, so everyone can create a topic in any group.

##### Migration guide

After deployment to hermes-management you need to run a migration task. 
It will initialise topic and subscription owners by assigning what used to be related group and subscription support teams.
Perform with admin credentials:

POST `/migrations/support-team-to-owner?source=Plaintext` (or `source=Crowd` if you used Crowd support teams)

### Enhancements

#### ([721](https://github.com/allegro/hermes/pull/721)) Creator must be an owner of created topic or subscription
#### ([714](https://github.com/allegro/hermes/pull/714)) Don't match when queried nested field doesn't exist instead of failing 
#### ([726](https://github.com/allegro/hermes/pull/726)) Pass Avro validation errors to users

### Bugfixes

#### ([717](https://github.com/allegro/hermes/issues/717)) NPE in Hermes frontend related to BlacklistZookeeperNotifyingCache
 
## 0.10.6 (07.02.2017)

### Features

#### ([611](https://github.com/allegro/hermes/pull/611)) Consumers rate negotiation

Max rate negotiation algorithm for balancing maximum delivery rate across subscription consumers.

### Enhancements

#### ([703](https://github.com/allegro/hermes/issues/703)) Update Curator dependency
#### ([701](https://github.com/allegro/hermes/pull/701)) Updated migration guide for 0.10.5
#### ([713](https://github.com/allegro/hermes/pull/713)) Admin scripts catalogue with initial migration script for 0.10.5

### Bugfixes

#### ([709](https://github.com/allegro/hermes/pull/709)) Fix docker-compose and docker setup

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
