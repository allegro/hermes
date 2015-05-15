Architecture
============

|
.. image:: /_static/hermes_top_view.png
    :height: 280px
    :width: 600px
    :align: center

|

Hermes is build on top of `Apache Kafka <http://kafka.apache.org/>`_ which implements `publish-subscribe pattern <http://en.wikipedia.org/wiki/Publish%E2%80%93subscribe_pattern>`_.

Messages in Hermes are published on topics and each topic can have many subscribing services.
Publishing messages, managing topics & subscriptions is done via REST API.

|
.. image:: /_static/architecture.png
    :height: 300px
    :width: 800px
    :align: center

|

Hermes modules:

* hermes-frontend
* hermes-consumers
* hermes-management
* broker - `Apache Kafka <http://kafka.apache.org/>`_
* metadata repository - `Apache Zookeeper <https://zookeeper.apache.org//>`_

Hermes frontend
---------------

Hermes-frontend is an entry point for publishing messages.

Frontend is designed to receive messages from publishers via HTTP protocol and send them to broker/Kafka.
Speed and low overhead was one of our main goals, therefor we decided to use `undertow <http://undertow.io/>`_  as an http server.
Messages are read asynchronously thanks to servlet API 3.0. After message is fully read it is sent to Kafka via Kafka producer.

Kafka producers
^^^^^^^^^^^^^^^

Frontend spawns two Kafka producers.


First producer is configured to receive ACK from topic partition leaders only (ACK 1). This is a fine compromise between
response speed and delivery guarantees. For topics where 99.9999% assurance is satisfactory, ACK 1 should be used.


Second producer works in ACK -1 mode, which means all nodes in partition ISR need to acknowledge that they received
and persisted message. This mode should be used, when we can't afford losing any message, no matter what happens on
Kafka. This comes at cost of longer response times.


The ACK mode can be set per topic. When you deal with vulnerable messages we recommend to set unclean.leader.election.enable flag to false on every broker.
This flag is available since kafka 0.8.2 version and more information about what it do can be found in `kafka documentation <http://www.kafka.apache.org/documentation.html#replication>`_

Buffer & async timeout tasks
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

After receiving contents of message from a publisher, message is put to Kafka producer internal sending queue/buffer.
We put (configurable) limits on request handling duration - by default 65ms. This means that we send our response to the
publisher at most after 65ms, giving us means to enforce strict SLA guarantees (at least for 99 and 99.9 percentiles).

If Kafka brokers are able to acknowledge message before request timeout, we send 201 Created response. If not, we still
contact publisher, sending him 202 Accepted status instead. This means, that we will try to deilver message to Kafka
asynchronously. Message lives in Kafka buffer and will be there until brokers acknowledge it. Our metrics indicate, that
only as little as 1% of messages receives 202 Accepted status, but this strongly depends on Kafka cluster perfromance.


Using 250Mb buffer, we are able to sustain ~1h Kafka outage with 3k requests per second. This of course varies depending
on message size.

Current drawback of this approach is that in case of node failure or process receiving SIGKILL, messages stored in buffer
memory are lost. We are already testing some buffer persistence internally and plan on publishing it as soon as it's stable.

Hermes consumers
----------------

Hermes-consumers module reads messages from topics and sends them to subscribers based on subscriptions metadata.

Consumers supervisor
^^^^^^^^^^^^^^^^^^^^

Consumers supervisor is a component in hermes-consumers module which listens on information about subscriptions.
Subscriptions are managed by hermes-management module and persisted in metadata repository (Apache Zookeeper).
Consumers supervisor is notified about every change on subscriptions.

When consumers supervisor is notified about new subscription, it creates new instance of Consumer. Each consumer operates
in separate thread, handling putting messages on send queue, retries and output rate adaptation.

Consumer
^^^^^^^^

Consumer reads messages from a topic specified in subscription via Kafka consumer. After message is read from the topic it is
forwarded to a MessageSender which sends it to an endpoint specified in subscription. Currently there are two MessageSenders
in Hermes:

#. JettyHttpMessageSender - sends messages to endpoint of type http (ex. ``http://sms-notifications.example.com:8080/user/confirm``)
#. HornetQJmsMessageSender - sends messages to endpoint of type jms (ex. ``jms://user:password@jms.example.com:5445/offerWatched``)

HermesConsumers builder allows adding custom MessageSender - see depolyment guide for details.

Retries
'''''''

Consumer retries sending message when receives response from subscriber with status code different than 2xx
(and optionally 4xx - configured on subscription) or IOException will be thrown.

Retries are done multiple times until message is received correctly or its TTL is exceeded.
Number of retries and delays between them is not easy to predict due to backoff mechanisms used.

Rate balancer
'''''''''''''

The **rate** field in subscription controls the maxium throughput (messages/second) of the subscriber's endpoint.
However Consumer can figure out if this value is not too high at the moment and can adjust speed of sending messages
to actual situation (i.e. subscribing service has slown down due to heavy GC)

If subscribing service stops receiving messages (returns 500 status code or is unreachable), Consumer runs rate limiting
algorithm to figure out sending speed that is right for subscriber. There are three states:

* normal - send messages with high speed, adapting to current service capabilities
* slow - send one request per second
* heartbeat - send one request per minute, poke if subscriber comes alive

Switching between modes is done using simple state machine as described below::

.. image:: /_static/consumer_state_machine.png
   :height: 250px
   :width: 400px
   :align: center

Hermes management
-----------------

Hermes management is a module responsible for managing groups, topics and subscriptions via REST API.
Performance is not a priority in this case. Load on that component is very low comparing to other modules in Hermes.
Management is build on `Spring Boot <http://projects.spring.io/spring-boot/>`_ and stores data in metadata repository/Zookeeper.

Metadata repository
-------------------

Every Hermes module uses `Apache Zookeeper <https://zookeeper.apache.org//>`_ as a metadata repository:

* Frontend module checks if topic exists basing on cached data from Zookeeper
* Consumers supervisor is notified about subscription changes. As a result it can do its work.
  Kafka consumers uses Zookeeper to (re)balance load from topics
* Management persists data in Zookeeper
* Broker/Apache Kafka also stores data in Zookeeper and uses it's notification mechanism about data changes

Hermes was designed with scalability in mind. It is possible to have multiple instances of every Hermes module because
they are based on Zookeeper and its notification mechanism.
