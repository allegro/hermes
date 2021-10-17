![Hermes logo](img/hermes.png)

Hermes is a message broker that greatly simplifies communication between services using publish-subscribe pattern.
It is HTTP-native, exposing REST endpoints for message publishing as well as pushing messages to subscribers REST endpoints.
Under the hood, [Apache Kafka](http://kafka.apache.org) is used.

## Easy to use

Hermes uses HTTP as a default communication protocol. This means that the only prerequisite to publish or receive
messages is to be able to send or consume HTTP requests. You can use Hermes to connect services written in different
technologies, there is nothing easier than generate or receive HTTP traffic.

Hermes takes care of message redelivery and uses adaptive algorithm to probe what is the optimal receiving rate for a
subscriber.

## Performance

Hermes was designed to leave as small footprint on top of Kafka as possible and we keep it at sub-millisecond level.
Performance of Hermes depends mostly on underlying Kafka cluster.

It was also designed to be easily scalable. Each component is stateless and can be spawned in multiple instances.

## Reliability

Hermes was designed to handle sensitive data, which needs highest guarantees of delivery. Using internal buffering
Hermes is able to operate and accept traffic while Kafka brokers are down. It also has a (configurable) guaranteed
maximum response time, which is kept even if Kafka is having trouble. Since Hermes is just a simple HTTP service, it
is sometimes much easier to keep it running than Kafka cluster.

## Measure everything

Hermes comes with a rich set of metrics. When publishing, observe latency and message sizes. When subscribing, see the
subscribers latency and response codes. These are just the most basic metrics that are gathered by default. Hermes is
not a black box.

Hermes has also powerful tracking mechanisms, which can help to debug more complicated problems at per-message level.
When clients have problems with sending or receiving messages, turn on the tracing and observe how each message flows
through the system.
