# Kafka and Zookeeper

## Zookeeper

Hermes uses Zookeeper as metadata store. It does not have to be the same Zookeeper as the one used by Kafka.

Option in Frontend/Consumers     | Option in Management           | Description                                                                | Default value
-------------------------------- | ------------------------------ | -------------------------------------------------------------------------- | --------------
zookeeper.connect.string         | storage.connectionString       | Zookeeper connection string                                                | localhost:2181
zookeeper.connection.timeout     | storage.connectTimeout         | connection timeout in seconds                                              | 10 000
zookeeper.max.retries            | storage.retryTimes             | retry count when connection fails                                          | 2
zookeeper.base.sleep.time        | storage.retrySleep             | time to wait between subsequent retries in seconds                         | 1 000
zookeeper.root                   | storage.pathPrefix             | prefix for Hermes data (if not specified in connection string)             | /hermes
zookeeper.cache.thread.pool.size | n/a                            | size of thread pool used by objects cache (like topics, subscriptions etc) | 5
zookeeper.authorization.enabled  | n/a                            | enable Zookeeper authorization                                             | false
zookeeper.authorization.scheme   | storage.authorization.scheme   | authorization scheme                                                       | digest
zookeeper.authorization.user     | storage.authorization.user     | username                                                                   | user
zookeeper.authorization.password | storage.authorization.password | password                                                                   | password
zookeeper.max.inflight.requests  | n/a                            | maximum number of unacknowledged requests before blocking                  | 10

## Kafka

### Single Kafka cluster

In simple case, Hermes is connected to just one Kafka cluster. Frontend and Consumers connect to Kafka to publish
and pull messages. Management connects to Kafka to manage existing topics and initiate retransmissions.

Frontend and Consumers options:

Option                         | Description                                                                              | Default value
------------------------------ | ---------------------------------------------------------------------------------------- | --------------
kafka.broker.list              | list of all brokers in the cluster (or at least some contact points); separated with ',' | localhost:9092
kafka.namespace                | namespace is a prefix prepended to all Kafka topics and consumer groups used by Hermes   | <empty>
kafka.zookeeper.connect.string | [Consumers only] connection string to Kafka Zookeeper                                    | localhost:2181
kafka.cluster.name             | name of Kafka cluster (relevant only when connecting to multiple clusters)               | primary

Zookeeper connection specific options (retries etc) are read from Metadata Zookeeper options.

Management module can connect to multiple Kafka clusters at once (see [section below](#multiple-kafka-and-zookeeper-clusters)), thus
when specifying connection option is done per cluster. Simple configuration for single cluster looks following:

```yaml
kafka:
  defaultNamespace: // namespace shared by all clusters, default: <empty>
  clusters:
    -
      clusterName: // name of cluster, can be any arbitrary string, default: primary
      connectionString: // connection string to cluster Zookeeper, default: localhost:2181
```

### Multiple Kafka and Zookeeper clusters

Hermes can be configured to publish and read messages to/from multiple Kafka clusters and to store metadata in multiple Zookeeper clusters.
We use this feature on production environment where we have separated kafka clusters in different data centers. If Kafka in one DC fails, whole traffic
can be routed to the second DC. This scenario assumes, that Kafka clusters hold different set of messages. There is no
support for multiple clusters each holding the same copy of data.

This is the schematics of two data center architecture:

![Multi DC schematics](../img/architecture-multi-cluster.png)

* there are specific **Frontend** and **Consumers** instances per cluster:
    * each **Frontend** instance writes data to single cluster
    * each **Consumers** instance reads data from single cluster
* each **Management** instance:
    * connects to all Kafka clusters and keeps topics and subscriptions in-sync
    * connects to all Zookeeper clusters and keeps metadata in-sync

Configuring Frontend and Consumers is easy: use configuration options from [previous chapter](#single-kafka-cluster) to
connect to given clusters. Remember about specifying proper `kafka.cluster.name`.

Since Management instances need to know all clusters, their configuration is bit more complex. Example configuration for
the schematics provided above:

```yaml
kafka:
  clusters:
    -
      datacenter: dc1
      clusterName: kafka_primary
      connectionString: kafka-zookeeper:2181/clusters/dc1
    -
      datacenter: dc2
      clusterName: kafka_secondary
      connectionString: kafka-zookeeper:2181/clusters/dc2

storage:
  pathPrefix: /run/hermes
  clusters:
    -
      datacenter: dc1
      clusterName: zk1
      connectionString: metadata-zookeeper.dc1:2181
    -
      datacenter: dc2
      clusterName: zk2
      connectionString: metadata-zookeeper.dc2:2181
```

### Multiple Hermes on single Kafka cluster

It’s also possible to run multiple Hermes clusters on a single Kafka cluster, e.g. to separate different test environments.
To do this, on each Hermes cluster you have to provide different value for:
* `kafka.namespace` property in **Frontend** and **Consumers**. In **Management** it’s named `kafka.defaultNamespace` and also need to be changed.
* `zookeeper.root` property in **Frontend** and **Consumers** if you use the same Zookeeper cluster for all Hermes clusters.
In **Management** it’s named `storage.pathPrefix` and also need to be changed.

`kafka.namespace` property also can used to distinguish Hermes-managed topics on multi-purpose Kafka cluster.

