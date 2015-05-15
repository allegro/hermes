Hermes Console
==============

    **Note** Hermes console has not been released as OpenSource yet, we do plan on doing it in near future.

Console is a web frontend which simplifies managing of groups, topics & subscriptions in Hermes. It is written in
`AngularJS <https://angularjs.org/>`_ and communicates with hermes-management module
via REST API. Information about how to install & configure Hermes console can be found in readme.md of the project.

Topic details
-------------

Example
^^^^^^^

.. image:: /_static/console_topic_details.png
   :height: 522px
   :width: 1169px
   :scale: 70%
   :align: center

Metrics description
^^^^^^^^^^^^^^^^^^^

* published - how many messages were published on topic since its creation
* rate - what is average speed of published messages/second on topic from last minute
* delivery rate - what is average speed of consumed messages/second from topic from last minute

Properties description
^^^^^^^^^^^^^^^^^^^^^^

* acknowledgment - how many brokers needs to acknowledge message received. Currently there are two options

  * leader only (default)
  * all brokers

* retention time - how many days messages must be persisted by brokers
* tracking enabled - Hermes for every published message returns messageId in response
  When tracking is enabled for a topic then messageId can be used to analyze what hermes-frontend module done with concrete message
* validation enabled - enables validation of every published message on that topic using specified json schema

Subscription details
--------------------

Example
^^^^^^^

.. image:: /_static/console_subscription_details.png
   :height: 890px
   :width: 1152px
   :scale: 70%
   :align: center

Metrics description
^^^^^^^^^^^^^^^^^^^

* delivery rate - what is average speed of delivered messages/second to subscriber from last minute
* delivered - how many messages were delivered to subscriber since subscription creation
* discarded - how many messages were discarded since subscription creation
  Message is marked as discarded when subscriber could not receive it (or rejected it) and it's ttl has expired.
* inflight - how many messages are in phase of sending to subscriber. Inflight is incremented when message was read from broker and
  decremented when message was delivered or discarded by subscriber

Properties description
^^^^^^^^^^^^^^^^^^^^^^

* rate limit - what is maximum speed for delivering messages/second to subscriber
* inflight TTL - how much time message sending must be retried before method will be marked as discarded
* tracking enabled - enables tracking every message on subscription in hermes-consumers module. See analogues flag in topic details
* retry client errors - if enabled (default true) then 4xx response status code from subscriber will be treated as error and message sending will be retried
