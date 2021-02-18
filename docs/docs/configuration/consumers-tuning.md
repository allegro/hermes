# Tuning Consumers

## HTTP Sender

Option                                               | Description                                                 | Default value
---------------------------------------------------- | ----------------------------------------------------------- | -------------
consumer.http.client.request.timeout                 | how much time we wait for client response before timing out | 1000ms
consumer.http.client.thread.pool.size                | size of thread pool for sender threads (global)             | 30
consumer.http.client.max.connections.per.destination | max connections per remote host                             | 100

## Consumers core

Option                        | Description                                                              | Default value
----------------------------- | ------------------------------------------------------------------------ | -------------
consumer.commit.offset.period | interval between committing offsets to Kafka                             | 20s
consumer.thread.pool.size     | thread pool for threads involved in consuming, 1 thread per subscription | 500
consumer.inflight.size        | how many messages can be kept in send queue, per subscription            | 100

## Workload constraints management

One of running consumers is a leader and this leader periodically rebalance consumers - assign/unassign subscriptions 
to/from available consumers.

After a subscription is created, it is assigned to specific consumers. By default each subscription is assigned to 
2 consumers that will deliver messages to the subscription and at the same time each consumer can have maximum 200 
subscriptions assigned to itself.

These numbers can be configured:

Option                                              | Description                               | Default value
--------------------------------------------------- | ----------------------------------------- | ---------------------
consumer.workload.consumers.per.subscription        | Number of consumers to which the subscription will be assigned. If this value is greater than the number of available consumers, Hermes will assign the subscription to all available consumers. | 2
consumer.workload.max.subscriptions.per.consumer    | The maximum number of subscriptions assigned to a single consumer. If all consumers have the maximum number of subscriptions assigned, a new subscription will not be activated until a new consumer is added or another subscription is unassigned. | 200

Additionally Hermes allows to configure the property `consumer.workload.consumers.per.subscription` for specific 
topics or subscriptions in the runtime via REST API. 

It is useful when in a system exist topics with huge traffic and other rarely used. On the other hand there could exist 
critical subscriptions in the system with the higher priority for low latency and less important subscriptions that accept
higher latency.

Creating workload constraints for topic:

`PUT /workload-constraints/topic`
```json
{
    "topicName": "pl.allegro.test.HugeTrafficTopic",
    "constraints": {
        "consumersNumber": 5
    }
}
```

A workload constraint specified for a topic is in fact the constraint for all subscriptions in this topic.

Creating workload constraints for subscriptions:

`PUT /workload-constraints/subscription`
```json
{
    "subscriptionName": "pl.allegro.test.HugeTrafficTopic$nonCriticalSubscription",
    "constraints": {
        "consumersNumber": 1
    }
}
```

Getting all defined workload constraints:

`GET /workload-constraints`
```json
{
  "topicConstraints": {
    "pl.allegro.test.HugeTrafficTopic": {
      "consumersNumber": 5
    }
  },
  "subscriptionConstraints": {
    "pl.allegro.test.HugeTrafficTopic$nonCriticalSubscription": {
      "consumersNumber": 1
    }
  }
}
```

These workload constraints are stored in ZooKeeper.

These operations could also be performed in Hermes Console. 
The dashboard to manage workload constraints is available at path: `/#/constraints`.
