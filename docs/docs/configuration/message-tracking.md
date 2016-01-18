# Message tracking storage

Hermes can store trace of each event pass through system for selected topics and subscriptions. Information stored in
trace are described in [subscribing guide](/user/subscribing). This section shows how to configure trace storage.

Trace data is important, but not critical in Hermes. The number one priority is to keep Hermes core functionality -
receiving and sending messages - stable. Thus in case of trace storage downtime or malfunction, internal queues might
overflow and information will be lost. Each time it happens logs are emitted. There are also metrics that allow monitoring
current trace message queue capacity.

## ElasticSearch

This is the preferred storage for traces. Trace information is append only and for optimal performance should be written
in batches, which is an ideal case for ElasticSearch.

Information is stored in daily-rolled indexes:

* **published_messages_[yyyy_MM_dd]**: contains traces from Frontend
* **sent_messages_[yyyy_MM_dd]**: contains traces from Consumers

They are accessed via **sent_messages** and **published_messages** aliases respectively. There is no index deletion
policy - it is up to ElasticSearch owner to implement one.

To use it, import `hermes-tracker-elasticsearch` module:

```groovy
compile group: 'pl.allegro.tech.hermes', name: 'hermes-tracker-elasticsearch', version: versions.hermes
```

in Frontend, Consumers and Management.

### Frontend configuration

* create `ElasicsearchClientFactory`, which will produce ElasticSearch driver and store it for cleanup
* set log repository via `HermesFrontend.Builder#withLogRepository`
* add shutdown hook to cleanup the driver

Example of usage with *example* configuration (there are no `config.get*` methods out of box!):

```java
ElasticsearchClientFactory elasticFactory = new ElasticsearchClientFactory(
    config.getInt(TRACKER_ELASTICSEARCH_PORT),
    config.getString(TRACKER_ELASTICSEARCH_CLUSTER_NAME),
    config.getString(TRACKER_ELASTICSEARCH_HOSTS)
);

builder.withLogRepository(serviceLocator ->
    new FrontendElasticsearchLogRepository.Builder(
            elasticFactory.client(),
            serviceLocator.getService(PathsCompiler.class),
            serviceLocator.getService(MetricRegistry.class)
        )
        .withClusterName(config.getStringProperty(KAFKA_CLUSTER_NAME))
        .withQueueSize(config.getInt(TRACKER_ELASTICSEARCH_QUEUE_CAPACITY))
        .withCommitInterval(config.getInt(TRACKER_ELASTICSEARCH_COMMIT_INTERVAL))
        .build()
);
builder.withShutdownHook(elasticFactory::close);
```

### Consumers configuration

Consumers module is configured in the same way as Frontend, except for log repository which is created using
`ConsumersElastisearchLogReposiory.Builder`.

### Management configuration

Make bean implementing `pl.allegro.tech.hermes.tracker.management.LogRepository` available in Spring context:

```java
@Bean
LogRepository logRepository(Client client) {
    return new ElasticsearchLogRepository(client);
}
```

## MongoDB

There is an option to use MongoDB as trace storage, but it is not recommended, at least with Mongo 2.x. We found that
even under relatively low load there were problems with writing batches of trace data and retrieving information.
