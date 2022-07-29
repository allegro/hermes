# Schema repository

Hermes requires an external schema repository in order to allow [publishing messages in Avro format](../user/publishing-avro.md).
Currently, there is only one implementation of message schema store available out of the box.

## Schema repository integration

### Confluent schema registry

Confluent schema registry is the recommended schema repository for Avro topics.

> Schema Registry provides a serving layer for your metadata. It provides a RESTful interface for storing and retrieving Avro schemas.
> It stores a versioned history of all schemas, provides multiple compatibility settings and allows evolution
> of schemas according to the configured compatibility setting. It provides serializers that plug into Kafka clients
> that handle schema storage and retrieval for Kafka messages that are sent in the Avro format.
> - [Schema Registry documentation](https://github.com/confluentinc/schema-registry)

Confluent schema registry is the default schema repository in Hermes.

## Configuration

Support for schema repository is always enabled and it cannot be turned off, but it is not required
to provide schema repository if Avro topics are not used.

### Frontend and Consumers

Frontend and Consumers modules have the same configuration options.

Option                                   | Description                                                        | Default value
-------------------------------------------------------- | ---------------------------------------------------------------------- | -------------
{modulePrefix}.schema.repository.serverUrl               | URL of a repository                                                    | `http://localhost:8888/`
{modulePrefix}.schema.repository.subjectSuffixEnabled    | Add `-value` suffix to every subject name                              | `false`
{modulePrefix}.schema.repository.subjectNamespaceEnabled | Add `kafka.namespace` property value as a prefix to every subject name | `false`
{modulePrefix}.schema.cache.refreshAfterWrite            | schema cache background refresh period                                 | 10m
{modulePrefix}.schema.cache.reloadThreadPoolSize         | how many background threads should maintain the cache                  | 2
{modulePrefix}.schema.cache.expireAfterWrite             | if schema can't be refreshed, it will be deleted after this period     | 24h

### Management

Option                                   | Description                                                        | Default value
---------------------------------------- | ------------------------------------------------------------------ | -------------
schema.repository.serverUrl              | URL of a repository                                                | `http://localhost:8888/`
schema.repository.validationEnabled      | Allows to use validation API in schema repository                  | `false`
schema.repository.connectionTimeoutMillis| Connection timeout used in http client (specified in milliseconds) | 1000
schema.repository.socketTimeoutMillis    | Read socket timeout used in http client (specified in milliseconds)| 3000
schema.repository.deleteSchemaPathSuffix | A suffix of the URL to delete all schema versions: `/subjects/{subject}/{deleteSchemaPathSuffix}| `versions`
schema.repository.subjectSuffixEnabled   | Add `-value` suffix to every subject name                          | `false`
schema.repository.subjectNamespaceEnabled| Add `kafka.defaultNamespace` property value as a prefix to every subject name | `false`

