Consumers module
================

Consumers module is responsible for reading messages from Kafka and pushing them to subscribers. It is distributed as
library jar.

Basic usage
-----------

In your ``build.gradle`` add::

    repositories {
        mavenCentral()
    }

    dependencies {
        compile group: 'pl.allegro.tech.hermes', name: 'hermes-consumer', version: 'hermes version'
    }

This gives you access to `HermesConsumers` object that should be used to kickstart consumers::

    public class MyCompanyHermesConsumers {

        public static void main(String... args) {

            // load ConfigFactory for sender
            ConfigFactory configFactory = new ConfigFactory();

            // load address resolver with interpolation feature
            InterpolatingEndpointAddressResolver interpolatingResolver = new InterpolatingEndpointAddressResolver(new MessageBodyInterpolator());

            // create httpClient to handle outgoing messages
            HttpClient httpClient = new HttpClientFactory(configFactory).provide();

            // create new consumers, that can handle http protocol messages
            HermesConsumers consumers = HermesConsumers.consumers()
                .withMessageSenderProvider("http", new JettyHttpMessageSenderProvider(
                    httpClient, configFactory, interpolatingResolver
            ));
            consumers.start();

            // you can also call consumers.stop() if you want to shutdown Hermes manually
        }
    }

You can register custom message senders. We provide tools to send ``http`` and ``jms`` (HornetQ) messages by default.

Configuration
-------------

Hermes consumers configuration is currently handled using Archaius. We only attached module responsible for reading
``.properties`` files. To point to custom properties, use ``archaius.configurationSource.additionalUrls`` env variable::

    java -jar mycompany-hermes-consumers.jar -Darchaius.configurationSource.additionalUrls=file:///etc/hermes/consumers.properties

Most of default values listed below have been tuned to our needs and are used on Hermes production.

Consumers core
^^^^^^^^^^^^^^

================================================ ======================================================================== =================
Property                                         Description                                                              Default value
================================================ ======================================================================== =================
consumer.commit.offset.period                    interval between committing offsets to Kafka                             20s
consumer.thread.pool.size                        thread pool for threads involved in consuming, 1 thread per subscription 500
consumer.inflight.size                           how many messages can be kept in send queue, per subscription            100
consumer.rate.limiter.supervisor.period          how often to run output rate adaptation algorithm 30 s
consumer.rate.limiter.reporting.thread.pool.size thread pool size for threads gathering send results (failed/success)     30
consumer.rate.limiter.slow.mode.delay            interval between sending messages in slow mode                           1s
consumer.rate.limiter.hearbeat.mode.delay        interval between sending messages in heartbeat mode                      60s
consumer.rate.convergence.factor                 rate limiting step                                                       0.2
consumer.rate.failures.ratio.threshold           ration of failed/success sends we tolerate without decreasing send speed 0.01
consumer.offset.monitor.enabled                  report difference between current message offset and Kafka head offset   true
consumer.status.health.port                      expose status message (ok/not ok) on this port                           8000
================================================ ======================================================================== =================


Distributed work balancing
^^^^^^^^^^^^^^^^^^^^^^^^^^

These options configure algorithm used to determine which consumer node should handle which active subscriptions.

Available algorithms
 * mirror - simplest mode which assumes that nodes are handling all active subscriptions (nodes are mirroring each other)
 * legacy.mirror - legacy version of mirroring mode, does not use any additional zk structures
 * selective - leader-based selective algorithm which attempts to spread work evenly among all possible nodes

Main principles of *selective* algorithm:
 1) subscriptions doesn't need to be handled by all consumer nodes in a cluster (only N number of consumers is enough)
 2) consumer nodes should be equally loaded but don't need to be fully loaded all the time
 3) single consumer node shouldn't have more responsibility that it can handle, if we have limited resources in a cluster we should fully utilize what we have and select only subset of subscriptions to handle


Configuration
"""""""""""""
================================================ ======================================================================================== ================= ================================
Property                                         Description                                                                              Algorithm         Default value
================================================ ======================================================================================== ================= ================================
consumer.workload.algorithm                      algorithm used to determine which consumer node should handle which active subscriptions                   legacy.mirror
consumer.workload.node.id                        identifies particular consumer node in a cluster environment                             mirror,selective  hostname with randomized suffix
consumer.workload.rebalance.interval.seconds     interval in seconds between rebalancing attempts                                         selective         30
consumer.workload.consumers.per.subscription     number of consumers that should handle single active subscription                        selective         2
consumer.workload.max.subscriptions.per.consumer maximum number of subscriptions that single consumer node can handle                     selective         ?
================================================ ======================================================================================== ================= ================================

HTTP sender
^^^^^^^^^^^

==================================================== =========================================================== =================
Property                                             Description                                                 Default value
==================================================== =========================================================== =================
consumer.http.client.request.timeout                 how much time we wait for client response before timing out 1000ms
consumer.http.client.thread.pool.size                size of thread pool for sender threads (global)             30
consumer.http.client.max.connections.per.destination max connections per remote host                             100
==================================================== =========================================================== =================

Kafka broker
^^^^^^^^^^^^

Make sure you connect your consumers to the same cluster that frontend publishes to.

================================== =========================================== =================
Property                           Description                                 Default value
================================== =========================================== =================
kafka.zookeeper.connect.string     Kafka Zookeeper connection string           localhost:2181
kafka.consumer.timeout.ms          connection timeout for partition consumer   60000
kafka.consumer.auto.offset.reset   offset reset method                         largest
================================== =========================================== =================

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
