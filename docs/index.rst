Hermes
======

  Hermes is a message broker which simplifies communication between services in a pub-sub manner.
  It is built on top of `Apache Kafka <http://kafka.apache.org/>`_ with REST API using high-performant
  `Undertow <http://undertow.io/>`_ server.


Performance and scalability
---------------------------

Hermes was created with huge performance requirements in mind. It is by design ready to run in the cloud and scale horizontally.
We spend a lot of time tuning the Undertow settings for lowest latency and best throughput. Hermes also takes advantage of Kafka producer
internal buffer which allows to respond within given timeout regardless of Kafka acknowledgment.

Currently we run Hermes in production handling up to 30.000 msgs/sec with 99.9th percentile latency below 100 ms on a 4-node cluster. This result
is tightly coupled with underlying Kafka performance.

Reliability
-----------

Most common usage of realtime stream system such as Kafka is subscribe to user activity, logs, metrics etc. Our use case is somehow
different - to use Hermes as a message broker for asynchronous communication between services. This means that apart from events listed above,
we send also more sensitive data, such as billing events, user account changes etc. Hermes allows to define more reliable policy for those
important events - require acknowledge from all Kafka replicas and increase request timeouts.

Push model
----------

Hermes uses concept of topic subscriptions - you specify an endpoint which would be invoked when event is published on a topic.
So event is pushed to a subscription unlike traditional model where consumer pulls the events from a topic.
It makes receiving messages from Hermes dead simple: you just have to write one HTTP endpoint in your service. It's up to Hermes
to create Kafka consumer, redeliver messages, keep eye on throughput limits etc.

Read more
---------

See how we change: :doc:`contents/changelog`.

Overview
********

Key concepts and performance.

.. toctree::
    :titlesonly:
    :glob:

    contents/overview/*

Tutorials
*********

.. toctree::
    :titlesonly:
    :glob:

    contents/tutorials/*

User documentation
******************

Hermes from client perspective.

.. toctree::
    :titlesonly:
    :glob:

    contents/user/*

Technical documentation
***********************

Running and administering own Hermes cluster.

.. toctree::
    :titlesonly:
    :glob:

    contents/tech/*
