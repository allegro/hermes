# Schema repository

Hermes requires an external schema repository in order to allow [publishing messages in Avro format](/user/publishing-avro).
Currently, there are two implementations of message schema store available out of box.

## Schema repository integrations

### Confluent schema registry

Confluent schema registry is the recommended schema repository for Avro topics.

> Schema Registry provides a serving layer for your metadata. It provides a RESTful interface for storing and retrieving Avro schemas.
> It stores a versioned history of all schemas, provides multiple compatibility settings and allows evolution
> of schemas according to the configured compatibility setting. It provides serializers that plug into Kafka clients
> that handle schema storage and retrieval for Kafka messages that are sent in the Avro format.
> - [Schema Registry documentation](https://github.com/confluentinc/schema-registry)

Set schema repository type to `schema_registry` when using this schema repository.

### Schema-repo store

This is another specialized schema repository, which has an option to append additional validations and checks when publishing
or updating schema (e.g. backwards compatibility, naming convention etc). Documentation can be found at
[project page](http://schemarepo.org).

Set schema repository type to `schema_repo` when using this schema repository.

## Configuration

### Frontend and Consumers

Frontend and Consumers module share the same configuration options. To enable schema-repo, set:

* `schema.repository.type`: `schema_registry` or `schema_repo`
* `schema.repository.url`: URL of repository

Additonal options:

Option                                   | Description                                                        | Default value
---------------------------------------- | ------------------------------------------------------------------ | -------------
schema.cache.refresh.after.write.minutes | schema cache background refresh period in minutes                  | 10
schema.cache.reload.thread.pool.size     | how many backgound threads should maintain the cache               | 2
schema.cache.expire.after.write.minutes  | if schema can't be refreshed, it will be deleted after this period | 60 * 24 (day)

### Management

Mandatory options:

* `schema.repository.type`: `schema_registry` or `schema_repo`
* `schema.repository.serverUrl`: URL of repository
