Message tracker
===============

As hermes works in push model, it would be nice to see history of individual messages for detailed debugging. In Hermes this feature is called "message tracker".
When event is published in Hermes it receives an UUID returned in ``Hermes-Message-Id`` header. You can then use this id to check what happened to event after publishing.

Message states
--------------

The state of message is separated for publishing on topic and delivering to consumer. Here is a list of possible states:

===================== ==================================================================
Publishing state      Description
===================== ==================================================================
SUCCESS               broker acknowledged that message was persisted
INFLIGHT              broker response timed out, so message is still in producer buffer
ERROR                 there was an error while sending message to broker
===================== ==================================================================

===================== ================================================================================================================================
Delivering state      Description
===================== ================================================================================================================================
INFLIGHT              message was consumed from broker and is ready to sent to subscription endpoint
SUCCESS               message was successfully delivered to subscription endpoint
FAILED                there was a fail (timeout, endpoint returned error) while delivering message, however message is still retried until TTL exceeds
DISCARDED             there were several failures in delivering message, TTL exceeded so message has not been delivered
===================== ================================================================================================================================

So a minimal successful state transition should be **SUCCESS** (publishing) -> **INFLIGHT** (consuming) -> **SUCCESS** (consuming).

Another possible flows:

* **INFLIGHT** (publishing) -> **SUCCESS** (publishing) -> **INFLIGHT** (consuming) -> **SUCCESS** (consuming)
* **SUCCESS** (publishing) -> **INFLIGHT** (consuming) -> **FAILED** (consuming) -> **SUCCESS** (consuming)
* **SUCCESS** (publishing) -> **INFLIGHT** (consuming) -> **FAILED** (consuming) (n times) -> **DISCARDED** (consuming)
* **INFLIGHT** (publishing) -> **ERROR** (publishing)

Configuration
-------------

To enable message tracking you have to configure at least one ``LogRepository`` from ``hermes-tracker`` module. Out of the box we provide two implementations:
`MongoDB <https://www.mongodb.org/>`_ and `Elasticsearch <https://www.elastic.co/products/elasticsearch>`_, and it's really simple to write your own.

Here is a sample ``HermesFronted`` wiring with Elasticsearch log repository enabled:

.. code-block:: java

    import pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchClientFactory;
    import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.ElasticsearchLogRepository;

    public class MyCompanyHermesFrontend {

        public static void main(String... args) {

            final ElasticsearchClientFactory elasticSearchClientFactory = new ElasticsearchClientFactory(9300, "my-elastic-cluster", "hy-cluster-host");
            HermesFronted frontend = HermesFronted.fronted()
                .withShutdownHook(elasticSearchClientFactory::close)
                .withLogRepository(serviceLocator -> new ElasticsearchLogRepository(elasticsearchClientFactory.client(), "primary"))
                .build();
            frontend.start();
        }
    }

Configuration of ``HermesConsumer`` is similar.

To read message trace from

.. code-block:: java

    @Configuration
    public class ElasticsearchLogRepositoryConfiguration {

        @Bean
        @ConditionalOnProperty("tracker.elasticsearch.enabled")
        LogRepository logRepository(Client client) {
            return new ElasticsearchLogRepository(client);
        }
    }

REST API
--------

Enable tracking
~~~~~~~~~~~~~~~

Message tracking can be enabled on individual topics and subscriptions. It is controlled by ``trackingEnabled`` property of topic/subscription.

To enable tracking on topic just send following ``PUT`` request::

    curl -X PUT -H "Content-Type: application/json" -d '{"trackingEnabled": "true"}' http://hermes-management-url/topics/com.example.my-topic

And for subscription::

    curl -X PUT -H "Content-Type: application/json" -d '{"trackingEnabled": "true"}' http://hermes-management-url/topics/com.example.my-topic/subscriptions/my-subscription

Display message trace
~~~~~~~~~~~~~~~~~~~~~

Given a valid ``Hermes-Message-Id`` (e.g. ``6b5af10f-7162-4787-9f66-de3d4baeb924``) you can check history of the message by doing this kind of request::

    curl http://hermes-management-url/topics/com.example.my-topic/subscriptions/my-subscription/events/6b5af10f-7162-4787-9f66-de3d4baeb924/trace

Sample response::

    [{
      "messageId":"6b5af10f-7162-4787-9f66-de3d4baeb924",
      "timestamp":1434487282863,
      "topicName":"com.example.my-topic",
      "status":"SUCCESS",
      "cluster":"primary"
    },
    {
      "messageId":"6b5af10f-7162-4787-9f66-de3d4baeb924",
      "timestamp":1434487282873,
      "subscription":"my-subscription",
      "topicName":"com.example.my-topic",
      "status":"INFLIGHT",
      "partition":4,
      "offset":1,
      "cluster":"primary"
    },
    {
      "messageId":"6b5af10f-7162-4787-9f66-de3d4baeb924",
      "timestamp":1434487283769,
      "subscription":"my-subscription",
      "topicName":"com.example.my-topic",
      "status":"FAILED",
      "reason":"Message sending failed with status code:500",
      "partition":4,
      "offset":1,
      "cluster":"primary"
    },
    {
      "messageId":"6b5af10f-7162-4787-9f66-de3d4baeb924",
      "timestamp":1434487293439,
      "subscription":"my-subscription",
      "topicName":"com.example.my-topic",
      "status":"DISCARDED",
      "reason":"Message sending failed with status code:500",
      "partition":4,
      "offset":1,
      "cluster":"primary"
    }]

Last undelivered
~~~~~~~~~~~~~~~~

If tracking on subscription is enabled, there is also a possibility to display last 100 undelivered messages::

    curl http://hermes-management-url/topics/com.example.my-topic/subscriptions/my-subscription/undelivered

Sample response::

    [{
      "messageId":"6b5af10f-7162-4787-9f66-de3d4baeb924",
      "timestamp":1434487293439,
      "subscription":"my-subscription",
      "topicName":"com.example.my-topic",
      "status":"DISCARDED",
      "reason":"Message sending failed with status code:500",
      "partition":4,
      "offset":1,
      "cluster":"primary"
    },
    {
      "messageId":"a42a0b6d-259c-430e-8a29-c23f5abf8450",
      "timestamp":1434488018563,
      "subscription":"my-subscription",
      "topicName":"com.example.my-topic",
      "status":"DISCARDED",
      "reason":"Message sending failed with status code:500",
      "partition":5,
      "offset":1,
      "cluster":"primary"
    },
    {
      "messageId":"9a27f441-88e3-4a39-a387-a3771f6888db",
      "timestamp":1434527586463,
      "subscription":"my-subscription",
      "topicName":"com.example.my-topic",
      "status":"DISCARDED",
      "reason":"Message sending failed with status code:500",
      "partition":2,
      "offset":1,
      "cluster":"primary"
    },
    {
      "messageId":"fac5f225-75a3-4c78-a918-e839a6743736",
      "timestamp":1434529649317,
      "subscription":"my-subscription",
      "topicName":"com.example.my-topic",
      "status":"DISCARDED",
      "reason":"Total timeout elapsed",
      "partition":3,
      "offset":1,
      "cluster":"primary"
    }]