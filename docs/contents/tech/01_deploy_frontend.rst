Frontend module
===============

Frontend module is responsible for handling incoming events and pushing them to Kafka. It is distributed as library
jar, which includes embedded Undertow server.

Basic usage
-----------

In your ``build.gradle`` add::

    repositories {
        mavenCentral()
    }

    dependencies {
        compile group: 'pl.allegro.tech.hermes', name: 'hermes-frontend', version: 'hermes version'
    }

This gives you access to `HermesFrontend` object that should be used to kickstart frontend::

    public class MyCompanyHermesFrontend {

        public static void main(String... args) {

            HermesFronted frontend = HermesFronted.fronted()
                .withStartupHook(/* custom initialization code */)
                .withShutdownHook(/* custom cleanup code */)
                .build();
            frontend.start();

            // you can also call frontend.stop() if you want to shutdown Hermes manually
        }

    }

Currently you can add handlers for:

* startup
* shutdown
* Kafka broker timeouts on sending event
* Kafka broker acknowledged event

Configuration
-------------

Hermes frontend configuration is currently handled using Archaius. We only attached module responsible for reading
``.properties`` files. To point to custom properties, use ``archaius.configurationSource.additionalUrls`` env variable::

    java -jar mycompany-hermes-frontend.jar -Darchaius.configurationSource.additionalUrls=file:///etc/hermes/frontend.properties

Most of default values listed below have been tuned to our needs and are used on Hermes production.

Frontend server
^^^^^^^^^^^^^^^

These properties are used to configure Undertow server that accepts incoming requests.

========================================== ================================================================================================ ==============
Property                                   Description                                                                                      Default value
========================================== ================================================================================================ ==============
frontend.port                              port to listen on                                                                                8080
frontend.idle.timeout                      how long should we wait before dropping idle connection for normal producer                      65 ms
frontend.long.idle.timeout                 how long should we wait before dropping idle connection for ACK-all producer                     400 ms
frontend.read.timeout                      how long wait for incoming data                                                                  2000 ms
frontend.request.parse.timeout             how long can request parsing take                                                                5000 ms
frontend.max.headers                       maximum number of headers we accept in single request                                            20
frontend.max.parameters                    maximum number of parameters we accept in single request                                         10
frontend.max.cookies                       maximum number of cookies we accept in single request                                            10
frontend.io.threads.count                  number of Undertow IO threads                                                                    2 * cores
frontend.worker.threads.count              number of worker threads                                                                         200
frontend.request.chunk.size                chunk size in bytes                                                                              1024
frontend.graceful.shutdown.initial.wait.ms when shutting down, period before setting status endpoint to DOWN and performing actual shutdown 10000 ms
frontend.http2.enabled                     enables HTTP/2 listener (requires setting all frontend.ssl.* properties)                         false
frontend.ssl.port                          secure port to listen on (enabled along with HTTP/2)                                             8443
frontend.ssl.protocol                      SSL protocol                                                                                     TLS
frontend.ssl.keystore.location             keystore location, prefixed with classpath: or file:                                             classpath:server.keystore
frontend.ssl.keystore.password             password required to access keystore                                                             password
frontend.ssl.keystore.format               keystore format                                                                                  JKS
frontend.ssl.truststore.location           truststore location, prefixed with classpath: or file:                                           classpath:server.keystore
frontend.ssl.truststore.password           password required to access truststore                                                           password
frontend.ssl.truststore.format             truststore format                                                                                JKS
========================================== ================================================================================================ ==============

The **idle.timeout** and **long.idle.timeout** timeouts are counted from the time the request has been parsed till
response is sent. This means, that interaction with Kafka needs to take place between this time.

If we reach timeout when event is in *sending to Kafka* state, we return **202 Accepted** response. Otherwise normal
*request timed out* message is sent. Depending on whether topic to which event is published has leader ACK or all ACK
mode, *normal* timeout and *long* timeouts are used respectively to minimize amount of *202* returned.

Kafka broker
^^^^^^^^^^^^

Most of Kafka broker properties map 1:1 to Kafka configuration options. See Kafka documentation if you have any
doubts or for extended description.

Remember that we create two Kafka producers with shared settings, so some resources (like buffer memory) are allocated
twice.

================================== ======================== =========================================== =================
Property                           Kafka config             Description                                 Default value
================================== ======================== =========================================== =================
kafka.broker.list                  BOOTSTRAP_SERVERS_CONFIG list of Kafka brokers to connect on startup localhost:9092
kafka.producer.metadata.max.age    METADATA_MAX_AGE_CONFIG  how old can topic metadata be               30000 ms
kafka.proudcer.compression.codec   COMPRESSION_TYPE_CONFIG  compression algorithm                       none
kafka.producer.retires             RETRIES_CONFIG           how many times should we retry sending      Integer.MAX_VALUE
kafka.producer.retry.backoff.ms    RETRY_BACKOFF_MS_CONFIG  backoff between retries                     256 ms
kafka.producer.buffer.memory       BUFFER_MEMORY_CONFIG     size of in-memory buffer in bytes           256 MB
kafka.producer.batch.size          BATCH_SIZE_CONFIG        size of sent message batch in bytes         16 kB
kafka.producer.tcp.send.buffer     SEND_BUFFER_CONFIG       size of TCP buffer                          128 kB
kafka.cluster                      -                        name of Kafka cluster when in multidc mode  primary
================================== ======================== =========================================== =================

Storage Zookeeper
^^^^^^^^^^^^^^^^^

These options configure Hermes zookeeper storage. Make sure they are the same as on Management and Consumer nodes.

================================ ========================================================== ==============
Property                         Description                                                Default value
================================ ========================================================== ==============
zookeeper.connect.string         connection string                                          localhost:2181
zookeeper.root                   prefix under which Hermes data is kept                     /hermes
zookeeper.connection.timeout     connection timeout                                         10000 ms
zookeeper.session.timeout        session timeout                                            10000 ms
zookeeper.max.retires            max connection retries                                     2
zookeeper.base.sleep.time        base time between connection retries, grows on each retry  1000 ms
zookeeper.cache.thread.pool.size size of thread pool used ot manage topics cache            5
================================ ========================================================== ==============

Message tracker
^^^^^^^^^^^^^^^

These options configure message tracking (for debug purpose mostly). Make sure they are the same as on Management and Consumer nodes.

=============================== ===================================================================== ========================================
Property                        Description                                                           Default value
=============================== ===================================================================== ========================================
tracker.mongodb.uri             mongo URI                                                             mongodb://localhost:27017/hermesMessages
tracker.mongodb.commit.interval push tracking messages to Mongo once per interval                     1000 ms
tracker.mongodb.queue.capacity  capacity of tracking messages queue - overflow messages are discarded 100 000
=============================== ===================================================================== ========================================

Metrics
^^^^^^^

These options configure metrics reporting, make sure they are the same as on Consumers nodes.

================================ ============================================= ==============
Property                         Description                                   Default value
================================ ============================================= ==============
metrics.zookeeper.reporter       should we report counter metrics to Zookeeper true
metrics.graphite.reporter        should we send all metrics to Graphite        false
metrics.console.reporter         should we print metrics as console output     false
graphite.host                    Graphite host                                 localhost
graphite.port                    Graphite port                                 2003
report.period                    how often should we send metrics to Graphite  20 s
================================ ============================================= ==============
