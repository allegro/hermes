# Metrics

Hermes gathers a big number of different metrics which are useful when trying to observe the current state of the system.

Latencies are measured as: 50, 75, 95, 99 and 99.9 percentiles.
Rates are measured and averaged in a time window. There are 3 time windows measured: 1, 5 and 15 minutes.

## Frontend

Frontend metrics are all prefixed with `producer.{hostname}`. Most of the metrics are collected in both aggregated and
per-topic scope.

### Latency

Latency metrics are grouped into two categories:

* ACK-leader acknowledgment level
* ACK-all acknowledgment level

In those categories it is possible to read both **broker latency** and **Hermes latency**. Broker latency measures
Kafka response times, while Hermes latency measures time span between receiving message till sending the response.

Metrics:

* `ack-all.broker-latency`
* `ack-all.broker-latency.{groupName}.{topicName}`
* `ack-all.latency`
* `ack-all.latency.{groupName}.{topicName}`
* `ack-leader.broker-latency`
* `ack-leader.broker-latency.{groupName}.{topicName}`
* `ack-leader.latency`
* `ack-leader.latency.{groupName}.{topicName}`

### Rate

Metrics:

* `meter`
* `meter.{groupName}.{topicName}`

### Response codes

These metrics measure global Hermes response codes. They make for good monitoring metrics, as sudden increase of **202**
or **500** status codes might signal an emergency. There are no per-topic metrics for response codes. See
[publishing guide](/user/publishing/#response-codes) for the meaning of response codes.

Metrics:

* `http-status-codes.code201`
* `http-status-codes.code202`
* `http-status-codes.code408`
* `http-status-codes.code500`

### Message

There are three metrics related to messages:

* **parsing time** which indicates how much time it took to receive the message
* **message size** in bytes
* **validation latency** which indicates how long did it took to validate message schema (if enabled)

Metrics:

* `parsing-request`
* `parsing-request.{groupName}.{topicName}`
* `message-size`
* `message-size.{groupName}.{topicName}`
* `validation-latency`
* `validation-latency.{groupName}.{topicName}`

### Buffers

These metrics indicate available [buffer](/user/publishing/#buffering) size for both ACK-all:

* `everyone-confirms-buffer-total-bytes`
* `everyone-confirms-buffer-available-bytes`

and ACK-leader buffers:

* `leader-confirms-buffer-total-bytes`
* `leader-confirms-buffer-available-bytes`

### Compression

When using Kafka compression algorithm, these metrics show average compression rate of messages:

* `everyone-confirms-compression-rate`
* `leader-confirms-compression-rate`

## Consumers

Consumers metrics are all prefixed with `consumer.{hostname}`. Most of the metrics are collected in both aggregated and
per-subscription scope.

### Subscription metrics

Hermes publishes a lot of metrics that can be useful when reasoning about subscribers health and debugging subscribers
issues:

* **latency** as measured by Hermes: from the start of sending the message till receiving response; path: `latency`
* **output rate**: effective output rate; path: `meter`
* **maximum output rate**: current maximum output rate as calulated by [Consumers rate limiter](/configuration/rate-limiting); path: `output-rate`
* **response statuses**: rate of different response statuses sent by client (2xx, 4xx, 5xx, timeouts and other failures); path: `status`

### Tracker

With tracing enabled, it is possible to observe the tracer queue size and remaining capacity:

* `tracker-queue-size`
* `tracker-remaining-capacity`
