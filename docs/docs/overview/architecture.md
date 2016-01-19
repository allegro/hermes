# Architecture

This chapter contains high view of Hermes architecture. It should give you basic idea of how Hermes operates and connects
to other systems. Each mentioned mechanism has own chapter that describes it in-depth, please refer to them for more
details.

Hermes is message broker using Kafka as message storage and routing backed. It consists of three modules:

* **Hermes Frontend** - receives traffic (messages) from clients, see: [publishing](/user/publishing)
* **Hermes Consumers** - sends messages to subscribers (push model), see: [subscribing](/user/subscribing)
* **Hermes Management** - manage topics and subscriptions

Hermes integrates with multiple systems, each having different role.

![Architecture overview](/img/architecture-overview.png)

* **Message Store** - stores and routes messages, current implementation: Kafka
* **Metadata Store** - shared metadata storage for all Hermes modules, current implementation: Zookeeper
* **Metrics Store** *[optional]* - stores metrics gathered by Hermes, current implementation: Graphite
* **Tracking Store** *[optional]* - stores tracking (message trace) information, current implementations: ElasticSearch, MongoDB

## Message flow

* *publisher* publishes message on given *topic* to **Frontend**
* **Frontend**:
    * message is assigned unique Hermes-Message-Id that can be used to track its way through the system
    * each action time is metered, metrics are sent to **Metrics Store**
    * if *topic* has tracing enabled, tracking information is sent to **Tracking Store**
    * message is sent to **Message Store**
* **Consumers** sends message to *subscriber*:
    * message is read from **Message Store**
    * each action time is metered, metrics are sent to **Metrics Store**
    * if *subscription* has tracing enabled, tracking information is sent to **Tracking Store**
    * message is sent to *subscriber*
    * in case of *subscriber* error, **Consumer** adjust sending speed and retries
