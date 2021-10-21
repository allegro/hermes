## 1.9.12 (21.10.2021)

## Enhancements

#### ([1407](https://github.com/allegro/hermes/pull/1407)) Add ace editor to schemaMessage field

Thanks to @sobelek for this contribution!

#### ([1399](https://github.com/allegro/hermes/pull/1399)) Fix documentation links, add markdown links check action

Thanks to @AleksanderBrzozowski for this contribution!

### Fixes

#### ([1401](https://github.com/allegro/hermes/pull/1401)) Fix linting and running tests in hermes console

Thanks to @sobelek for this contribution!

#### ([1406](https://github.com/allegro/hermes/pull/1406)) Topic and subscription with '-' char in name can be updated

## 1.9.11 (18.10.2021)

## Enhancements

#### ([1395](https://github.com/allegro/hermes/pull/1395)) Update readme with nodemon to auto reload files durning development
#### ([1394](https://github.com/allegro/hermes/pull/1394)) Support json formatting when debugging filters in hermes-console

Thanks to @sobelek for these contributions!

#### ([1393](https://github.com/allegro/hermes/pull/1393)) Automatically adding .* in the front and in the end of a search query

Thanks to @MaciejAndrearczyk for this contribution!

#### ([1392](https://github.com/allegro/hermes/pull/1392)) Disable retransmission if subscription is not active

Thanks to @akrystian for this contribution!

#### ([1381](https://github.com/allegro/hermes/pull/1381)) Remove ui/index.html from hermes-console url 

Thanks to @platan for this contribution!

#### ([1379](https://github.com/allegro/hermes/pull/1379)) Cohere retention time validation between backend and frontend

Thanks to @dominikbrandon for this contribution!

#### ([1386](https://github.com/allegro/hermes/pull/1386)) Char '-' is not allowed in topic name when adding a new one.

### Fixes

#### ([1389](https://github.com/allegro/hermes/pull/1389)) Fixed registration of ReadOnlyFilter

## 1.9.10 (05.10.2021)

## Enhancements

#### ([1376](https://github.com/allegro/hermes/pull/1376)) Moved disabling metric attributes to config

#### ([1375](https://github.com/allegro/hermes/pull/1375)) Added warning when setting tracking on topic and subscription

#### ([1374](https://github.com/allegro/hermes/pull/1374)) Clarified conditions to control overall sending speed on subscription

### Fixes

#### ([1378](https://github.com/allegro/hermes/pull/1378)) Serve hermes-console by NodeJS

#### ([1377](https://github.com/allegro/hermes/pull/1377)) Support unicode in filter debug in hermes-console

## 1.9.9 (14.09.2021)

## Enhancements

#### ([1371](https://github.com/allegro/hermes/pull/1371)) Added subscriptionName to description for SubscriptionHealthProblem

## 1.9.8 (09.09.2021)

## Enhancements

#### ([1370](https://github.com/allegro/hermes/pull/1370)) HttpRequestFactory interface extracted 

## 1.9.7 (24.08.2021)

## Enhancements

#### ([1369](https://github.com/allegro/hermes/pull/1369)) Subscription zookeeper metrics endpoint

## 1.9.6 (23.08.2021)

## Fixes

#### ([1361](https://github.com/allegro/hermes/pull/1367)) Add RestTemplate bean for EventAuditor

## 1.9.4 (09.08.2021)

## Fixes

#### ([1361](https://github.com/allegro/hermes/pull/1361)) Topic name is now published in tags by MicrometerTaggedMetricsProvider

Changed MeterRegistry interface by adding topic name as functions parameter. Renamed `MicrometerMetricsProvider` to `MicrometerTaggedMetricsProvider`,
also changed implementation by not pushing topic (which is also provided in tags now) and all tags name to path.
It changes metrics provided by Micrometer e.g. from this: `hermes-client.com_group.topic.status.{code}` with tags: `{code="201"}` to this: `hermes-client.status` with tags: `{code="201", topic="com_group.topic"}`

## 1.9.3 (27.07.2021)

## Enhancements

#### ([1359](https://github.com/allegro/hermes/pull/1359)) Add support for publication of audit events

### Fixes

#### ([1362](https://github.com/allegro/hermes/pull/1362)) Refactored benchmark module

## 1.9.2 (14.07.2021)

## Enhancements

#### ([1353](https://github.com/allegro/hermes/pull/1353)) Configurable request timeout in subscription view

## 1.9.1 (29.06.2021)

### Fixes

#### ([1348](https://github.com/allegro/hermes/issues/1348)) Fixed subscription creation via UI

## 1.9.0 (15.06.2021)

## Enhancements

#### ([1346](https://github.com/allegro/hermes/pull/1346)) Add configurable filtering rate limiter

#### ([1345](https://github.com/allegro/hermes/pull/1345)) Add UI panel for http header filters 

## 1.8.9 (02.06.2021)

## Enhancements

#### ([1342](https://github.com/allegro/hermes/pull/1342)) Add kafka producer publishing error metrics

## 1.8.8 (26.05.2021)

## Enhancements

#### ([1340](https://github.com/allegro/hermes/pull/1340)) Renamed HostnameResolver to InstanceIdResolver

## 1.8.7 (05.05.2021)

## Enhancements

#### ([1339](https://github.com/allegro/hermes/pull/1339)) Groups can now be created without admin rights by default. This behaviour (both button visibility in console and role checking in management) can be changed by modifying "nonAdminCreationEnabled" property in console and management.

## 1.8.6 (13.04.2021)

### Enhancements

#### ([1336](https://github.com/allegro/hermes/pull/1336)) Added headers provider for batch sender

## 1.8.5 (29.03.2021)

### Enhancements

#### ([1334](https://github.com/allegro/hermes/pull/1334)) Removed deprecated _all field from ES queries

## 1.8.4 (26.03.2021)

### Enhancements

#### ([1333](https://github.com/allegro/hermes/pull/1333)) Allow configuring percentiles on MeterRegistry level in hermes-client
#### ([1332](https://github.com/allegro/hermes/pull/1332)) Pluggable sending result handlers
#### ([1330](https://github.com/allegro/hermes/pull/1330)) Topic retention in hours
#### ([1326](https://github.com/allegro/hermes/pull/1326)) Added support for custom AvroEnforcer and MessageConverterResolver

Thanks to @tadamcze for this contribution!

## 1.8.3 (05.03.2021)

### Fixes

#### ([1328](https://github.com/allegro/hermes/pull/1328)) Prevent closing unsaved modals

## 1.8.2 (26.02.2021)

### Fixes

#### ([1287](https://github.com/allegro/hermes/pull/1287)) Fix jshint config and previously not detected errors

Thanks to @pwolaq for this contribution!

### Enhancements

#### ([1322](https://github.com/allegro/hermes/pull/1322)) Optimized getting list of unhealthy subscriptions

#### ([1320](https://github.com/allegro/hermes/pull/1320)) Bump elasticsearch client version

#### ([1318](https://github.com/allegro/hermes/pull/1318)) Bump tools versions to fix vulnerabilities

Thanks to @adrian-warcholinski for this contribution!

#### ([1319](https://github.com/allegro/hermes/pull/1319)) Added an option to set response delay in Hermes Mock

Thanks to @platan for this contribution!

#### ([1280](https://github.com/allegro/hermes/pull/1280)) Returning more detailed message when published event does not match a schema

Thanks to @jewertow for this contribution!

#### ([1285](https://github.com/allegro/hermes/pull/1285)) Avoid clearing unsaved forms

Thanks to @pwolaq for this contribution!

#### ([1316](https://github.com/allegro/hermes/pull/1316)) Allow header propagation just by using configuration

Thanks to @althink for this contribution!

## 1.8.1 (01.12.2020)

### Fixes

#### ([1313](https://github.com/allegro/hermes/pull/1313)) Defined labels UI fix

## 1.8.0 (01.12.2020)

### Enhancements

#### ([1258](https://github.com/allegro/hermes/pull/1258)) Introduce Reactive Hermes Client

Thanks to @wjur for this contribution!

#### ([1293](https://github.com/allegro/hermes/pull/1293)) Added possibility to tag topics with defined labels

Thanks to @kacperb333 for this contribution!

## 1.7.0 (18.11.2020)

### Enhancements

#### ([1299](https://github.com/allegro/hermes/pull/1299)) Add new meaningful metrics to MetricsMessageDeliveryListener

New hermes-client metrics take into account also application level errors which are represented by http status codes.

`Topic.publish.finally.success:` Message has been accepted by Hermes

`topic.publish.finally.failure:` Message has been rejected by Hermes or there was transport level issue. 

`topic.publish.failure:` Number of failures including retries.

`topic.publish.attempts:` Number of publish attempt. Does not include retries.

`topic.publish.retry.success:` Number of successful publications after one or more retry.

`topic.publish.retry.failure:` Number of failures after retries have been exhausted.

`topic.publish.retry.attempt:` Number of publications where retry was involved.


#### ([1301](https://github.com/allegro/hermes/pull/1301)) partition-key HTTP header

Added ability to publish messages to the specific partition


#### ([1310](https://github.com/allegro/hermes/pull/1310)) hermes-frontend change default response http status

This improvement is related to this [enhancement](https://github.com/allegro/hermes/blob/master/CHANGELOG.md#1294-hermes-frontend-occasionally-returns-incorrect-status-200).
Now hermes-frontend changes default response status from 200 to 500 for every exchange. 

### Fixes

#### ([1304](https://github.com/allegro/hermes/issues/1304)) Json validation fix in debugging filters in hermes-console


## 1.6.2 (9.11.2020)

### Fixes

#### ([1305](https://github.com/allegro/hermes/pull/1305)) Moved `consistency checker` config to Properties class

#### ([1300](https://github.com/allegro/hermes/pull/1300)) Fixed publish to Sonatype by use of `Nexus Publish Plugin`

## 1.6.1 (2.11.2020)

### Enhancements

#### ([1296](https://github.com/allegro/hermes/pull/1296)) Consistency checking in hermes-console

From now on Hermes operator can check consistency between multiple Hermes Zookeeper clusters via Hermes-console.
Inconsistency can occur when one Zookeeper cluster is down for a while.

#### ([1294](https://github.com/allegro/hermes/pull/1294)) Hermes frontend occasionally returns incorrect status 200

In Hermes-frontend there are only two [status codes](https://hermes-pubsub.readthedocs.io/en/latest/user/publishing/#response-codes)
representing success, these are 201 and 202. Unfortunately, Hermes-frontend occasionally returned 200.
It’s because it’s built on Undertow http server which returns 200 http status code as default one when response isn’t set.
Thanks this enhancement instead of 200, 500 is returned. 500 means error and can be retried by a client.

#### ([1256](https://github.com/allegro/hermes/pull/1256)) Readonly mode for Kafka topics configuration

#### ([1279](https://github.com/allegro/hermes/pull/1279)) Button for copying schema of a topic

#### ([1283](https://github.com/allegro/hermes/pull/1283)) Changed order of operations on topic creation

## 1.6.0 (15.10.2020)

### Enhancements

#### ([1277](https://github.com/allegro/hermes/pull/1277)) Removed usage of all Kafka Zookeeper clients

It's now possible not to define connectionString for Kafka Zookeeper in Hermes configuration, as all usages
of Kafka Zookeeper clients have been removed.

## 1.5.4 (13.10.2020)

### Enhancements

#### ([1270](https://github.com/allegro/hermes/pull/1270)) A topic change updates its all cached schemas

#### ([1271](https://github.com/allegro/hermes/pull/1271)) Audit logs for unsuccessful topic and subscription operations

#### ([1273](https://github.com/allegro/hermes/pull/1273)) Topic removal is forbidden if the topic contains any subscription

#### ([1274](https://github.com/allegro/hermes/pull/1274)) Moved filtering to hermes-common

#### ([1275](https://github.com/allegro/hermes/pull/1275)) Added message filters debugger

### Fixes

#### ([1276](https://github.com/allegro/hermes/pull/1276)) Downgraded kafka to 2.1.1

Kafka dependency needs to be kept in this version due to bug that occurs when using kafka dependency in versions 
2.2.0 - 2.3.1 and brokers in versions before 2.4.0 (https://issues.apache.org/jira/browse/KAFKA-9212). 

Ideally, we would bump kafka dependency up to 2.3.2, which according to the issue above is no longer affected by this bug,
but it's nowhere to be found :(. Since 2.4.0 version bumps transitive dependency for a zookeeper client library, it would introduce
further incompatibility if Hermes Zookeeper is in lower version than Zookeeper used in Kafka, as these two are independent
of each other.

This needs to be revisited after Hermes is completely independent of kafka library, as no transitive dependency 
to zookeeper will be present (kafka-clients library is free of transitive zookeeper dependency).

#### ([1268](https://github.com/allegro/hermes/pull/1268)) Fixed http headers now have a dedicated pane in console 

## 1.5.3 (28.08.2020)

### Enhancements

#### ([1263](https://github.com/allegro/hermes/pull/1263)) Magic byte truncation for schema version
Ignoring magic byte and schema version when schema version present in a header

## 1.5.2 (25.08.2020)

### Enhancements

#### ([1250](https://github.com/allegro/hermes/pull/1250)) Optimizing graphite metrics queries

### Fixes

#### ([1251](https://github.com/allegro/hermes/pull/1251)) Fix HermesMockExtension stops wiremock server after first test class

## 1.5.1 (18.08.2020)

### Enhancements

#### ([1244](https://github.com/allegro/hermes/pull/1244)) Remove support for schema-repo

#### ([1247](https://github.com/allegro/hermes/pull/1247)) Preserving filter value in groups list

### Fixes

#### ([1246](https://github.com/allegro/hermes/pull/1246)) Fix SchemaNotFoundException returns 5xx

#### ([1248](https://github.com/allegro/hermes/pull/1248)) Subscription name validation fixed

## 1.4.14 (17.07.2020)

### Fixes

#### ([1243](https://github.com/allegro/hermes/pull/1243)) Fix for serialization with schema id

## 1.4.13 (15.07.2020)

### Enhancements

#### ([1232](https://github.com/allegro/hermes/pull/1232)) Serialization with schema id

Issues resolved: [1225 - Confluent message serialization and deserialization compataibility](https://github.com/allegro/hermes/issues/1225)
and [682 - Avro messages does not contain header](https://github.com/allegro/hermes/issues/682).

#### ([1235](https://github.com/allegro/hermes/pull/1235)) Docker update

Dockerfiles have been updated. The project is now automatically built by dockerhub.

#### ([1236](https://github.com/allegro/hermes/pull/1236)) Batch subscription disabled in hermes console

#### ([1237](https://github.com/allegro/hermes/pull/1237)) Feature: default schema id serialization

#### ([1238](https://github.com/allegro/hermes/pull/1238)) Hermes docs update

Exponential retry policy and docker guide added to docs.

#### ([1240](https://github.com/allegro/hermes/pull/1240)) Schema registry added to docker

### Fixes

#### ([1239](https://github.com/allegro/hermes/pull/1239)) Old vagrant file removed from docker dir

## 1.4.12 (07.07.2020)

### Fixes

#### ([1234](https://github.com/allegro/hermes/pull/1234)) Fix for exponential retry policy

Fixed hermes console for batch subscription.

## 1.4.11 (01.07.2020)

### Enhancements

#### ([1223](https://github.com/allegro/hermes/pull/1223)) Remove ALPN for jdk 1.8.0_252+

Removing APLN for jdk 1.8.0_252. More info [here](https://webtide.com/jetty-alpn-java-8u252/).

#### ([1224](https://github.com/allegro/hermes/pull/1224)) Handle proper broker listener name

#### ([1228](https://github.com/allegro/hermes/pull/1228)) Kafka libraries upgraded to version 2.2.2

### Fixes

#### ([1222](https://github.com/allegro/hermes/pull/1222)) Configurable namespace operator

Namespace operators are now cofigurable. Fix for [1209 - Feature/schema subject naming](https://github.com/allegro/hermes/pull/1209).

## 1.4.10 (23.06.2020)

### Enhancements

#### ([1221](https://github.com/allegro/hermes/pull/1221)) Exponential retry policy

Exponential retry policy for failed requests in serial subscription.

## 1.4.9 (19.06.2020)

### Enhancements

#### ([1220](https://github.com/allegro/hermes/pull/1220)) Update config for management

## 1.4.8 (19.06.2020)

### Enhancements

#### ([1214](https://github.com/allegro/hermes/pull/1214)) Read only mode persistence

Read only mode set by an admin (READ_ONLY_ADMIN) in Hermes Management will be not overwritten by healthcheck task.

#### ([1215](https://github.com/allegro/hermes/pull/1215)) Return error response when owner not found

Instead of `200 OK` with empty list it returns `404 Not Found` `{"message":"Owner of id 'yyyy' not found in source xxx","code":"OWNER_NOT_FOUND"}` when owner not found.

#### ([1216](https://github.com/allegro/hermes/pull/1216)) Fix typo: happend -> happen

#### ([1217](https://github.com/allegro/hermes/pull/1217)) Adding modules find and register by default

It is required to use annotations like `@JsonCreator` / `@JsonProperty` for classes written in Kotlin.

This patch fixes redundant Jackson annotations.

#### ([1218](https://github.com/allegro/hermes/pull/1218)) Update Hermes management config structure

## 1.4.7 (09.06.2020)

### Enhancements

#### ([1208](https://github.com/allegro/hermes/pull/1208)) Schema repository docs improvement
#### ([1209](https://github.com/allegro/hermes/pull/1209)) Feature/schema subject naming
#### ([1210](https://github.com/allegro/hermes/pull/1210)) Add missing documentation of workload constraints
#### ([1203](https://github.com/allegro/hermes/pull/1203)) Hermes secured

Allow Hermes to connect to the Kafka via secured channel. This feature provides additional configs params:

- `kafka.authorization.enabled` - `true` or `false` (default is `false`)
- `kafka.authorization.mechanism` - mechanism for authentication (default is `PLAIN`)
- `kafka.authorization.protocol` - protocol for the communication (default is `SASL_PLAINTEXT`)
- `kafka.authorization.username` - client's username (default is `username`)
- `kafka.authorization.password` - client's password (default is `password`)

### Fixes

#### ([1202](https://github.com/allegro/hermes/pull/1202)) Fixed Vagrant provisioning

Added fallback to `archive.apache.org` for Kafka distribution.

#### ([1207](https://github.com/allegro/hermes/pull/1207)) java11 fix: jaxb deps upgrade

#### ([1212](https://github.com/allegro/hermes/pull/1212)) Removed duplicated counter for filtered messages

Removed duplicated counter for filtered messages.

## 1.4.6 (24.04.2020)

### Enhancements

#### ([#1196](https://github.com/allegro/hermes/pull/1196)) Hermes supports JRE truststore

Hermes supports using trust store provided by JRE (located in `$JAVA_HOME/jre/lib/security/`).

This change introduces new properties `{consumers,frontend}.ssl.{key,trust}store.source` which can be set to `jre` or `provided`.

Additionally the property `consumer.ssl.enabled` is enabled by default.

### Fixes

#### ([#1197](https://github.com/allegro/hermes/pull/1197)) Remove logback files from hermes-consumers and hermes-frontend

## 1.4.5 (20.04.2020)

### Enhancements

#### ([#1199](https://github.com/allegro/hermes/pull/1199)) Remove limits for offline retention time

#### ([#1198](https://github.com/allegro/hermes/pull/1198))  Adds deserialization root cause exception in Hermes-mock

### Fixes

#### ([#1194](https://github.com/allegro/hermes/pull/1194)) Fix typo in docs 

## 1.4.4 (31.03.2020)

### Enhancements

#### ([1192](https://github.com/allegro/hermes/pull/1192)) Schema `__metadata` field validation

Hermes Management does not allow to save topic with an invalid Avro schemas:
- schema without field __metadata
- schema with field __metadata that declares types other than hermes specifies in documentation, for example `java.avro.string`

#### ([1189](https://github.com/allegro/hermes/pull/1189)) Unwrapping message content interface

This change introduce the interfaces `MessageContentReader` and `MessageContentReaderFactory`.

These interfaces allow to provide custom implementation of reading Kafka records, for example for reading metadata from Kafka headers.

It can be useful when you publish messages directly on Kafka and use only consuming module from Hermes.

For more information, see the [docs](https://hermes-pubsub.readthedocs.io/en/latest/configuration/internal-format/#custom-reading-internal-messages).

## 1.4.3 (24.03.2020)

### Enhancements

#### ([1186](https://github.com/allegro/hermes/pull/1186)) Added metrics for http client connection pool

Added http connection pool monitoring. By default it’s disabled, can be enabled by flag:

`consumer.http.client.connection.pool.monitoring.enabled=true`

#### ([1188](https://github.com/allegro/hermes/pull/1188)) Max requests queued per destination config param for http clients

`consumer.inflight.size` is no longer used in HTTP clients configuration as max requests queued
 per destination. Instead, `consumer.http.client.max.connections.per.destination` and `consumer
 .http2.client.max.connections.per.destination` can be used to configure that parameter.

## 1.4.2 (24.03.2020)

### Bugfixes

### Enhancements

#### ([1183](https://github.com/allegro/hermes/pull/1183)) Retry on HTTP 429 Too many requests code

From now Hermes provides back pressure mechanism relaying only on 503 and new 429 http status header.

#### ([1180](https://github.com/allegro/hermes/pull/1180)) Fix subscription latency URL
#### ([1182](https://github.com/allegro/hermes/pull/1182)) Restoring node command in th path

## 1.4.1 (12.03.2020)

### Enhancements

#### ([1177](https://github.com/allegro/hermes/pull/1177)) HttpClients can have additional headers set

Additional HTTP headers can be set by providing custom implementation of HttpHeadersProvidersFactory interface.

#### ([1178](https://github.com/allegro/hermes/pull/1178)) Sending delay is calculated based on message publishing timestamp


## 1.4.0 (22.02.2020)

The release contains a lot of improvements created during Hacktoberfest event.
Many thanks to contributors for implementing them, great work! :tada:

### Enhancements

#### ([1173](https://github.com/allegro/hermes/pull/1173)) Metadata headers in messages by @mareckmareck
#### ([1012](https://github.com/allegro/hermes/pull/1012)) Catching all Throwables when consuming messages by @dankraw
#### ([1012](https://github.com/allegro/hermes/pull/1012)) Allowed to filter messages by any element in array by @karolhor
#### ([1166](https://github.com/allegro/hermes/pull/1166)) Hermes-management serves console config from application property file by @druminski
#### ([1138](https://github.com/allegro/hermes/pull/1138)) Added UI for HTTP header filtering by @qrman
#### ([1107](https://github.com/allegro/hermes/pull/1107)) Hermes-console served by Hermes-management as static resource by @mkopylec
#### ([1165](https://github.com/allegro/hermes/pull/1165)) Removed mirror algorithm in consumer workload mechanism by @jewertow
#### ([1127](https://github.com/allegro/hermes/pull/1127)) Manually create consumer group/commit offsets on subscription creation by @jewertow
#### ([1141](https://github.com/allegro/hermes/pull/1141)) Added sorting by name and search for constraints UI by @pwolaq
#### ([1114](https://github.com/allegro/hermes/pull/1114)) Hermes-console UX improvements by @krzysztofpcy
#### ([1100](https://github.com/allegro/hermes/pull/1100)) Report consumer sender workload by @jewertow
#### ([1124](https://github.com/allegro/hermes/pull/1124)) Return proper response when Avro lacks __metadata by @jewertow
#### ([1137](https://github.com/allegro/hermes/pull/1137)) Introduced list of supported topic content types by @semisiu
#### ([1139](https://github.com/allegro/hermes/pull/1139)) Redirects in hermes-consumers are disabled by default by @druminski
#### ([1158](https://github.com/allegro/hermes/pull/1158)) Exposed creation and modification date for topic & subscription by @jewertow
#### ([1162](https://github.com/allegro/hermes/pull/1162)) Removed Jersey repackaged immutable map reference by @dankraw

## 1.3.5 (27.01.2020)

This version contains important changes related to Java 11 deployment of Hermes frontends
(especially a critical fix in Undertow regarding the use of TLSv1.3 in JDK11)
as well as some performance improvements (in terms of GC impact) of the way Avro API is being used.

### Enhancements

#### ([1160](https://github.com/allegro/hermes/pull/1160)) Using Undertow 2 on frontends
#### ([1156](https://github.com/allegro/hermes/pull/1156)) Reusing Avro binary decoders

### Bugfixes

#### ([1153](https://github.com/allegro/hermes/pull/1153)) Fixed Hermes mock predicate

## 1.3.4 (27.11.2019)

### Enhancements

#### ([1149](https://github.com/allegro/hermes/pull/1149)) Hermes Mock asserts contentType using startsWith check

Thanks @Deff17 for contribution.

### Bugfixes

#### ([1147](https://github.com/allegro/hermes/pull/1147)) Added support for IE 11 in Hermes Console

Thanks @kuaikuai for contribution.

## 1.3.3 (04.11.2019)

### Enhancements

#### ([1134](https://github.com/allegro/hermes/pull/1134)) Including schema validation error details in management responses

### Bugfixes

#### ([1132](https://github.com/allegro/hermes/pull/1132)) Fixed url of service mock in QuickStart guide

#### ([1146](https://github.com/allegro/hermes/pull/1146)) Fixed consumer process stopping on subscription removal

#### ([1140](https://github.com/allegro/hermes/pull/1140)) Clearing inflight-messages meter on consumer stop

#### ([1131](https://github.com/allegro/hermes/pull/1131)) Renamed file with key dedicated for integration tests

#### ([1130](https://github.com/allegro/hermes/pull/1130)) Fixed hierarchical max rate registry test

## 1.3.2 (21.10.2019)

### Enhancements

#### ([1104](https://github.com/allegro/hermes/pull/1104)) Bump Apache AVRO to 1.9.0 and json-avro-converter to 0.2.9

## 1.3.1 (15.10.2019)

### Features

#### ([1103](https://github.com/allegro/hermes/pull/1103)) Unhealthy subscriptions filtering

Added two new parameters to `/unhealthy` management endpoint that additionally allow filtering the returned list of unhealthy subscriptions by:

* subscriptions names
* qualified topic names

Example:

```
http://{hermes-management}/unhealthy?ownerSourceName=Service%20Catalog\
&ownerId={service_id}&respectMonitoringSeverity=false\
&subscriptionNames={subscription_names}&qualifiedTopicNames={qualified_topic_names}
```

### Enhancements

#### ([1122](https://github.com/allegro/hermes/pull/1122)) Change order of role verification in management

This should allow admins to control topic and subscriptions management regardless proper ownership being configured.

### Bugfixes

#### ([1118](https://github.com/allegro/hermes/pull/1118)) Fixing frontends waiting for kafka behaviour when there are no topics

When using  `frontend.startup.wait.kafka.enabled=true` feature, in situation when there where no topics created in kafka yet, frontends would wait indefinitely for topics metadata to become available.

#### ([1125](https://github.com/allegro/hermes/pull/1125)) Remove stale assignments from cluster assignment cache

Cluster assignment cache wasn't properly cleared from previous assignments when using `flat-binary` workload registry type, and it could cause rebalance job to behave unstable.

## 1.3.0 (1.10.2019)

### Features

#### ([1110](https://github.com/allegro/hermes/pull/1110)) Flat binary storage for consumers workload
Introducing more concise registry type for consumers workload distribution that should help scale better. 
Each consumer uses a single znode that contains binary encoded list of subscriptions that the consumer should process.
The configuration loads fast and is updated only on workload distribution change.
Enabled with `consumer.workload.registry.type=flat-binary` setting. The default is `hierarchical` type.

#### A single consumer registry and leader election
Consumer registry is extracted from consumer workload and is now used by max-rate job as well. 
The registry contains a leader latch which is always enabled and available.

#### ([1095](https://github.com/allegro/hermes/pull/1095)) Removal of deprecated `StrictMaxRateProvider`
The legacy max-rate provider type is now removed.

#### Removal of inflight message counter
The inflight message counter as well as the distributed zookeeper counter are now removed.
This feature was not used but was leaving a lot of junk in zookeeper.

#### ([1106](https://github.com/allegro/hermes/pull/1106)) Consumer constraints management in hermes-console
This feature allows easy management of consumer constraints. Link to it is not visible in the home screen as it is an admin feature 
(all endpoints are admin-secured though), accessed from `http://<hermes-console>/#/constraints` URL.

#### ([1113](https://github.com/allegro/hermes/pull/1113)) Frontends wait for kafka when booting up
Frontends will not start the HTTP server unless the underlying kafka brokers are available, i.e. we can fetch topics metadata from them. 
By default the feature is disabled, enable with `frontend.startup.wait.kafka.enabled=true`.

#### ([1109](https://github.com/allegro/hermes/pull/1109)) Cancel all waiting messages on stopping sender
When a subscription is stopped all messages that were already accepted by consumer message sender will be now dropped.

## 1.2.5 (27.09.2019)

### Features

#### ([1009](https://github.com/allegro/hermes/issues/1009)) Disable dynamic reloading of configuration files
Enabling possibility of providing external archaius configuration.
Pulling archaius initialization from `ConfigFactory` out and initializing `DynamicPropertyFactory` at the start of `hermes-consummers` and `hermes-frontend`. 
Restore the default way that archaius builds configuration with the addition of a configurable option to enable/disable config reload, by default is disabled.

## 1.2.4 (23.09.2019)

### Features

#### ([1096](https://github.com/allegro/hermes/pull/1096)) Failed messages metrics
New client metrics have been added for failed messages that won't be retried:
- hermes-client.*.retries.exhausted - the number of unsent messages, max retries limit reached
- hermes-client.*.retries.success - the number of retried messages with success
- hermes-client.*.retries.attempts - how many retries the message required before success delivery
- hermes-client.*.retries.count - the number of retried messages

#### ([1009](https://github.com/allegro/hermes/issues/1009)) Disable dynamic reloading of configuration files
Hermes uses archaius library (https://github.com/Netflix/archaius) for configuration management.
By default, it tracks configuration changes at the given intervals. This mechanism has been disabled.

## 1.2.3 (10.09.2019)

### Features

#### ([1083](https://github.com/allegro/hermes/pull/1083)) Workload constraints

Workload constraints allow to configure consumers number for subscription or topic.

Constraints are stored in zookeeper. hermes-management exposes REST API for constraints management.

- create or update topic constraints
```
PUT /workload-constraints/topic
```
```json
{
  "topicName": "group.topic",
  "constraints": {
    "consumersNumber": 4
  }
}
```

- delete topic constraints
```
DELETE /workload-constraints/topic/group.topic
```

- create or update subscription constraints
```
PUT /workload-constraints/subscription
```
```json
{
  "subscriptionName": "group.topic$subscription",
  "constraints": {
    "consumersNumber": 4
  }
}
```

- delete subscription constraints
```
DELETE /workload-constraints/subscription/group.topic/subscription
```

### Bugfixes

#### ([1094](https://github.com/allegro/hermes/pull/1094)) hermes-mock matches content-type header

### Enhancements

#### ([1089](https://github.com/allegro/hermes/pull/1089)) Topics metadata refreshed in a background thread

#### ([1092](https://github.com/allegro/hermes/pull/1092)) Speed up Ports.nextAvailable

## 1.2.2 (22.08.2019)

### Features

#### ([1084](https://github.com/allegro/hermes/pull/1084)) `WebClient` Hermes sender implementation

Introduces an implementation of Hermes Sender which makes use of reactive, non-blocking 
[WebFlux HTTP client](https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-client).

#### ([1086](https://github.com/allegro/hermes/pull/1086)) Flat tree with binary-content nodes implementation of max-rate registry

Introducing a lightweight alternative of max-rate storage which generates less zookeeper nodes and should load fast 
even for large number of subscriptions and consumer nodes.

Enable this storage type with `consumer.maxrate.registry.type: flat-binary` property in hermes-consumers configuration
(previous `hierarchical` storage type is used as default).

## 1.2.1 (05.08.2019)

### Features

#### ([1076](https://github.com/allegro/hermes/pull/1076)) `SchemaRepositoryInstanceResolver` interface introduced

This change allows providing different instance resolvers for schema repositories in frontends and consumers. 
The default implementations uses configured URLs, but one can provide an implementation that 
makes use of a custom discovery mechanism or instance hashing.

### Enhancements

#### ([1073](https://github.com/allegro/hermes/pull/1073)) Original message available on `HermesResponse` when it wasn't published

#### ([1079](https://github.com/allegro/hermes/pull/1079)) Hermes client supports case insensitive headers

### Bugfixes

#### ([1063](https://github.com/allegro/hermes/pull/1063)) `Keep-alive` excluded in HTTP/2 communication in hermes-consumers

## 1.2.0 (24.07.2019)

### Enhancements

#### ([1060](https://github.com/allegro/hermes/pull/1060)) Added support for JDK 11

#### ([1071](https://github.com/allegro/hermes/pull/1071)) Counter values summed from all zookeeper clusters

Counters published, delivered, discarded and volume are kept in zookeeper. Thanks to this their values aren't reset after Hermes restart. 
However, recently multi-zookeeper feature was introduced in Hermes allowing to have independent Hermes Zookeeper clusters per DC. 
Because of this when a client sends a request about some topic or subscription metrics to hermes-management then it receives 
metrics summed from all zookeeper clusters.

#### ([1069](https://github.com/allegro/hermes/pull/1069)) Added storage health check metrics

Since we have [#1052](https://github.com/allegro/hermes/issues/1052), the next step was to introduce metrics 
for storage (zookeeper clusters) health checks. They are added to MeterRegistry as `storage-health-check.successful` 
and `storage-health-check.failed` counters.

## 1.1.2 (08.07.2019)

### Enhancements

#### ([1061](https://github.com/allegro/hermes/pull/1061)) Introduced HdrHistogram-based metrics reservoir

Introduced [HdrHistogram](http://hdrhistogram.org/)–based implementation of reservoir in metrics. HdrHistogram-based 
reservoir has much less memory footprint than exponentially decaying (previous implementation). Because of this GC in 
Hermes has less job to do which leads to better Hermes performance. In the near future it will be default Hermes metrics reservoir.

To switch between implementations, use `metrics.reservoir.type` option:

* `exponentially_decaying` - exponentially decaying reservoir (default)
* `hdr` - HdrHistogram–based reservoir 

## 1.1.1 (05.07.2019)

### Enhancements

#### ([1052](https://github.com/allegro/hermes/issues/1052)) Auto switching to read only mode in hermes-management 

Hermes-management verifies whether all zookeeper clusters are available. 

It writes periodically a timestamp to each one of them. 

When the timestamp write fails on one of the zk clusters then management switches into ReadOnly mode.

This feature is disabled by default. Enable with:

```yaml
management:
  health:
    periodSeconds: 30
    enabled: true
```

## 1.1.0 (02.07.2019)

### Enhancements

#### ([1033](https://github.com/allegro/hermes/pull/1033)) Zookeeper multi datacenter

With the multi Zookeeper clusters, it's possible to increase Hermes isolation between data-centres.

In the end this change increase high-availability of Hermes.

Changes are only in hermes-management module which from now on can operate on multiple Zookeeper clusters.

To match Kafka clusters with Zookeeper clusters we have an additional field in a hermes-config called `datacenter`.

This field allows you to tell Hermes which Zookeeper cluster belongs to which Kafka cluster.

When there is only one Zookeeper cluster then it will be used for every Kafka cluster.

If you're migrating from previous version you need to add `storage.clusters` field in hermes-config and move `storage.connectionString` inside it (`datacenter` field is irrelevant when you have only 1 Zookeeper cluster).

Example:
```yaml
kafka:
  clusters:
    -
      datacenter: dc1
      clusterName: kafka_primary
      connectionString: kafka-zookeeper:2181/clusters/dc1
    -
      datacenter: dc2
      clusterName: kafka_secondary
      connectionString: kafka-zookeeper:2181/clusters/dc2

storage:
  pathPrefix: /run/hermes
  clusters:
    - 
      datacenter: dc
      clusterName: zk
      connectionString: zookeeper:2181
```

## 1.0.7 (01.07.2019)

### Bugfixes

#### ([1053](https://github.com/allegro/hermes/pull/1053)) Maximum possible offset to be committed for partition

## 1.0.6 (28.06.2019)

### Enhancements

#### ([1048](https://github.com/allegro/hermes/pull/1048)) Exposed volume metric in metrics query endpoint
#### ([1045](https://github.com/allegro/hermes/pull/1045)) Enabled caching for dependencies on Travis

### Bugfixes

#### ([1050](https://github.com/allegro/hermes/pull/1050)) Catch exceptions thrown while stopping kafka consumers

## 1.0.5 (25.06.2019)

### Enhancements

#### ([1042](https://github.com/allegro/hermes/pull/1042)) Consumer group diagnostics tab in console
#### ([1041](https://github.com/allegro/hermes/pull/1041)) Added volume counter

### Bugfixes

#### ([1040](https://github.com/allegro/hermes/pull/1040)) Clearing stale offsets, partition assignment term introduced

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
