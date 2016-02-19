# Subscribing

Hermes uses **push** model to send messages from broker to subscribers. Hermes takes care of retrying, throttling and
all other little details. What subscriber needs to do, is create endpoint that will accept HTTP POST request.
Subscription is always created in context of a topic. Subscriber will receive only messages published after subscription
was created.

## Creating subscription

Use Hermes Management REST API to create subscription by sending POST request with `application/json` content type
on topics `subscriptions` resource:

```
/topics/{topicName}/subscriptions
```

Body of request must contain at least:

* name: name of subscription
* endpoint: valid URI
* supportTeam: name of team that owns the subscription

Minimal request:

```json
{"name": "mySubscription", "endpoint": "http://my-service", "supportTeam": "My Team"}
```

All options:

Option                               | Description                      | Default value
------------------------------------ | -------------------------------- | -------------
trackingEnabled                      | track incoming messages?         | false
subscriptionPolicy.rate              | maximum sending speed in rps     | 100
subscriptionPolicy.messageTtl        | inflight Time To Live in seconds | 3600
subscriptionPolicy.retryClientErrors | retry on receiving 4xx status    | false

Request that specifies all available options:

```json
{
    "name": "mySubscription",
    "endpoint": "http://my-service",
    "description": "This is my subscription",
    "trackingEnabled": false,
    "supportTeam": "My Team",
    "contact": "my-team@my-company.com",
    "subscriptionPolicy": {
        "rate": 100,
        "messageTtl": 3600,
        "retryClientErrors": false
    }
}
```

## Suspending subscription

It is possible to suspend any subscription. This means, that no messages will be sent, but the information about last
consumed message is preserved. After reactivating subscription, sending starts from the point where it stopped.

To change subscription status send POST request with `application/json` content type:

```
/topics/{topicName}/subscriptions/{subscriptionName}/state
```

with new status name in body (quotation marks are important!):

* to **suspend**: `"SUSPENDED"`
* to **activate**: `"ACTIVE"`

## Failure & success

Hermes treats any response with **2xx** status code as successful delivery (e.g. 200 or 201).

Responses with **5xx** status code or any network issues (e.g. connection timeout) are always treated as failures.

Responses with **4xx** status code are treated as failures, but by default they are not retried. This is because
usually when subscriber responds with *400 Bad Message* it means this message is somehow invalid and will never be parsed,
no matter how many times Hermes would try to deliver it. This behavior can be changed using **retryClientErrors**
flag on subscription.

## Retries

Hermes Consumers have been optimized towards maximizing chances of successful message delivery. Retry policy is
simple for the client to grasp, as it is configured using only two parameters: **Inflight Time To Live (TTL)**
and **Retry backoff**.

**Inflight TTL** is specified in seconds and it limits for how long the message will be kept inflight - read out of
Kafka, but not delivered to subscriber yet. During this period of time Hermes Consumers tries to deliver the message.
In case of failure, next delivery is scheduled after minimum of **retry backoff** time, which is specified in milliseconds.
Message offset will not be committed to broker unless it's retry limit has been exhausted (it has been delivered or discarded).

How many times a message can be retried? Very rough calculation can be made using this formula:

```
retries = to_millis(inflight_ttl) / retry_backoff
```

This calculation does not take rate limiting into a count, so answer to this question is much more complicated.

We decided to use time-based configuration (as opposed to specifying retry count), because the malfunctions
of subscribing services are time constrained in one way or another. It is much easier to state, that in case of service
failure the rescue team has one hour to fix the problem before any event will be discarded.

By default inflight TTL is set to 3600 seconds (an hour) and retry backoff is set to 100ms.
We set a hard limit for the inflight TTL to 7200 seconds (two hours).

## Back pressure

The client is able to signal it can't handle the message at the moment and Hermes Consumer will retry delivery after
minimum of given delay.

The endpoint can return **Retry-After** header, with the amount of seconds to backoff, combined with status **503**.

Regardless of the provided delay, the **Inflight TTL** of the message still applies in this situation,
therefore the endpoint needs to ensure the total delay of consecutive **Retry-After** responses does not exceed this value.
In case it does, the message is discarded.

An important limitation to remember is that the offset won't be committed until the message is either
successfully delivered or discarded and in case of consumer failure all messages following (even when successfully processed)
will be resent.

### Last undelivered message

It is possible to easily retrieve contents of last undelivered message, along with timestamp and reason why it could
not be delivered.

```
/topics/{topicName}/subscriptions/{subscriptionName}/undelivered/last
```

It will return **404 Not Found** if there is no message to display. Otherwise it information in following format:

```json
{
    "timestamp": 1452952981548,
    "subscription": "subscription-name",
    "topicName": "group.topic-name",
    "status": "DISCARDED",
    "reason": "Total timeout elapsed",
    "message": "body of a message",
    "partition": 5,
    "offset": 368741824,
    "cluster": "primary"
}
```

Partition, offset and cluster specify the position of this message in Kafka, in case it was needed to retrieve it or
to start the retransmission.

## Rate limiting

Each subscription can define a hard limit of accepted messages per second and Hermes will never cross this line. However
below this treshold, rate limiting algorithm tries to match sending speed with current capabilities of subscriber.

For example lets take subscriber A who has declared that he is able to receive 100 msg/sec at maximum. Hermes will be
sending messages at this rate. Now assume that there is a problem with subscriber A - 10% of requests gets timed out.
Hermes will lower the speed up to the moment, when it sees no dropped requests. When subscriber A has dealt with problems,
the speed will automatically increase to reach the maximum.

This is important when trying to understand why subscriber receives less messages than expected or the subscribers lag
is growing. First things first, you should check subscription metrics for signs of any problems.

If you want to know the exact algorithm, check [rate limiting configuration page](/configuration/rate-limiting/).

## Metrics

Subscription metrics are available at:

```
/topics/{topicName}/subscriptions/{subscriptionName}/metrics
```

and include:

* **delivered**: number of delivered messages through the lifetime of subscriptions
* **discarded**: number of delivered messages through the lifetime of subscriptions
* **inflight**: number of messages currently in send Buffer
* **timeouts**: number of requests per second that end in subscribing service timeout
* **otherErrors**: number of requests per second that end in some other network error
* **codes2xx**: number of requests per second that end in returning 2xx HTTP response
* **codes4xx**: number of requests per second that end in returning 4xx HTTP response
* **codes5xx**: number of requests per second that end in returning 5xx HTTP response
* **rate**: current sending rate

## Retransmission

Hermes gives an option to easily retransmit messages that are still available on Kafka. Simply send POST to:

```
/topics/{topicName}/subscriptions/{subscriptionName}/retransmission
```

The message body can be of following format:

* ISO date: `2015-09-03T13:30:00.000`
* number of hours to retransmit: `-6h`

Hermes will find message offset in Kafka, that is closes to the given date and initiate retransmission. In return, you will
receive list of offsets from which retransmission will be started per partition.

## Message tracking

When message tracking is enabled on subscription, it is possible to display message flow through Hermes by using its
**Hermes-Message-Id**. Endpoint:

```
/topics/{topicName}/subscriptions/{subscriptionName}/events/{Hermes-Message-Id}/trace
```

returns list of message status changes as it flew through Hermes.

```json
[
    {
        "messageId": "1d6b9496-5af7-4a06-a27f-df7a6a5719c6",
        "timestamp": 1452955332961,
        "topicName": "topic-name",
        "status": "INFLIGHT",
        "cluster": "cluster"
    },
    {
        "messageId": "1d6b9496-5af7-4a06-a27f-df7a6a5719c6",
        "timestamp": 1452955332967,
        "topicName": "topic-name",
        "status": "SUCCESS",
        "cluster": "cluster"
    },
    {
        "messageId": "1d6b9496-5af7-4a06-a27f-df7a6a5719c6",
        "timestamp": 1452955332967,
        "subscription": "subscription-name",
        "topicName": "topic-name",
        "status": "INFLIGHT",
        "partition": 5,
        "offset": 171165098,
        "cluster": "cluster"
    },
    {
        "messageId": "1d6b9496-5af7-4a06-a27f-df7a6a5719c6",
        "timestamp": 1452955332979,
        "subscription": "subscription-name",
        "topicName": "topic-name",
        "status": "FAILED",
        "reason": "Total timeout elapsed",
        "partition": 5,
        "offset": 171165098,
        "cluster": "cluster"
    },
    {
        "messageId": "1d6b9496-5af7-4a06-a27f-df7a6a5719c6",
        "timestamp": 1452955332979,
        "subscription": "subscription-name",
        "topicName": "topic-name",
        "status": "SUCCESS",
        "partition": 5,
        "offset": 171165098,
        "cluster": "cluster"
    }
]
```

Depending on the progress, different kind of information is gathered. In the example above, first two traces were written
by **Frontend**. Trace from Frontend contains only timestamp and message id. Last three traces originated in **Consumers**,
and contain additional information, such as delivery status, subscription which received the message and exact position
in Kafka. Consumers trace contains information about each attempt to deliver the message.

Possible statuses for **Frontend** traces are:

* **INFLIGHT**: message has been received, but not acknowledged by Kafka yet, it waits in the buffer
* **SUCCESS**: message has been received and acknowledged by Kafka
* **FAILED**: i don't think we ever seen this status on production

Possible statuses for **Consumers** traces are:

* **INFLIGHT**: message has been read from Kafka and is in sender queue
* **SUCCESS**: message has been successfully delivered
* **FAILED**: this attempt of sending the message failed, it will be retired
* **DISCARDED**: message delivery failed

### Last 100 undelivered messages

With message tracking enabled, it is also possible to list last 100 undelivered messages, as opposed to only last one
without message tracking. This information is available at:

```
/topics/{topicName}/subscriptions/{subscriptionName}/undelivered
```

It returns array of message tracking information in following format:

```json
[
    {
        "messageId": "1d6b9496-5af7-4a06-a27f-df7a6a5719c6",
        "timestamp": 1452955332980,
        "subscription": "subscription-name",
        "topicName": "topic-name",
        "status": "DISCARDED",
        "reason": "Message sending failed with status code:400",
        "cluster": "primary",
        "offset": 171165098,
        "partition": 5
    }
]
```
