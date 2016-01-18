# Schema repository

Hermes can use message schema to validate incoming messages on a topic, as described in
[publishing guide](/user/publishing#message-schema). There are two implementations of message schema store available
out of box.

## Simple schema store

This is the simple, default implementation which stores schema information along with topic information inside
Metadata Store (Zookeeper). There are no additional schema checks (except for initial vaildation) in place.

## Schema-repo store

This is remote, specialized schema repo, which has an option to append additional validations and checks when publishing
or updating schema (e.g. backwards compatibility, naming convention etc). Documentation can be found at
[project page](http://schemarepo.org).

This schema repository needs to be enabled and configured in all modules.

### Frontend and Consumers

Frontend and Consumers module share the same configuration options. To enable schema-repo, set:

* `schema.repository.type`: `schema_repo`
* `schema.repository.url`: URL of repository

Additonal options:

Option                                   | Description                                                        | Default value
---------------------------------------- | ------------------------------------------------------------------ | -------------
schema.cache.refresh.after.write.minutes | schema cache background refresh period in minutes                  | 10
schema.cache.reload.thread.pool.size     | how many backgound threads should maintain the cache               | 2
schema.cache.expire.after.write.minutes  | if schema can't be refreshed, it will be deleted after this period | 60 * 24 (day)

### Management

Mandatory options:

* `schema.repository.type`: `schema_repo`
* `schema.repository.serverUrl`: URL of repository
