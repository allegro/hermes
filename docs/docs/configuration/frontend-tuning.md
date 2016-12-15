# Tuning Frontend

Frontend module is crucial for the client-perceived performance and stability of Hermes. There are many configuration
options that can be changed to fine tune the [Undertow HTTP server](http://undertow.io) and communication with Kafka.

## HTTP Server

Hermes Frontend can be configured to send the response after no longer than specific timeout. This prevents long-running
requests in case of Kafka downtime and helps to reason about service SLA. This is related to
[response codes](/user/publishing/#response-codes): depending on whether message has been persisted in Kafka before the
timeout, publisher receives **201** or **202** response code.

You can change the timeout values separately for messages with different [acknowledgment levels](/user/publishing/#acknowledgment-level).
To do so, use

* `frontend.idle.timeout` for leader ACK
* `frontend.long.idle.timeout` for all ACK

Those timeouts are counted from the time the request has been parsed till response is sent. This means, that
interaction with Kafka needs to take place during this period. If timeout is reached when event is in
*sending to Kafka* state, **202 Accepted** response is returned, since message is already in sending buffer and Hermes
guarantees it will end up in Kafka. Otherwise normal *request timed out* message is sent.


Option                                     | Description                                                                          | Default value
------------------------------------------ | ------------------------------------------------------------------------------------ | -------------
frontend.port                              | port to listen on                                                                    | 8080
frontend.read.timeout                      | maximum time to wait for first portion of data to appear on socket                   | 2000ms
frontend.request.parse.timeout             | maximum time to wait before full request is received                                 | 5000ms
frontend.request.chunk.size                | chunk size                                                                           | 1024B
frontend.io.threads.count                  | number of Undertow IO threads                                                        | 2 * cores
frontend.worker.threads.count              | number of Undertow worker threads                                                    | 200
frontend.graceful.shutdown.initial.wait.ms | time between setting health endpoint to return DOWN and actually stopping the server | 10 000ms

Default timeout settings make Frontend safe against [Slowloris attack](https://en.wikipedia.org/wiki/Slowloris_(software)).


## Buffers

Hermes uses Kafka in-memory send buffers to tolerate any downtime or hiccups, as described in
[publishing guide](/user/publishing/#buffering). Use `kafka.producer.buffer.memory` option to change the size of buffer.
By default it is set to **256 MB**. Changing the size might extend the period for which Hermes is able to receive
messages in case of Kafka downtime. Since internally there are two Kafka producers spawned, one for ACK-leader and one
for ACK-all, there are also **two buffers** - keep this in mind when deciding on heap size.

## Kafka

Kafka producer properties map 1:1 to Kafka producer configuration options. See Kafka documentation if you have any doubts
or need an extended description.

Option                           | Kafka config            | Description                            | Default value
-------------------------------- | ----------------------- | -------------------------------------- | -------------
kafka.producer.metadata.max.age  | METADATA_MAX_AGE_CONFIG | how old can topic metadata be          | 30000 ms
kafka.proudcer.compression.codec | COMPRESSION_TYPE_CONFIG | compression algorithm                  | none
kafka.producer.retires           | RETRIES_CONFIG          | how many times should we retry sending | Integer.MAX_VALUE
kafka.producer.retry.backoff.ms  | RETRY_BACKOFF_MS_CONFIG | backoff between retries                | 256 ms
kafka.producer.batch.size        | BATCH_SIZE_CONFIG       | size of sent message batch in bytes    | 16 kB
kafka.producer.tcp.send.buffer   | SEND_BUFFER_CONFIG      | size of TCP buffer                     | 128 kB

## Graceful startup

Processing time for first event arriving on each topic may be longer than expected from Hermes frontend. 
This is because topic metadata has to be fetched from Kafka. What is more, Avro topics need schema in order to validate incoming messages.

To get rid of this issue and reduce event processing latency, all the required data can be fetched during Hermes frontend's startup, 
just before it's healthcheck goes green.
Note, enabling startup data loading will make frontend boot a little longer.

Option                                                   | Description                                             | Default value
-------------------------------------------------------- | ------------------------------------------------------- | -------------
frontend.startup.topic.metadata.loading.enabled          | should the startup topic metadata loading be enabled    | false
frontend.startup.topic.metadata.loading.retry.interval   | retry interval between retry loops                      | 1s
frontend.startup.topic.metadata.loading.retry.count      | number of retries between topic metadata fetch loops    | 5
frontend.startup.topic.metadata.loading.thread.pool.size | number of worker threads loading metadata concurrently  | 16
frontend.startup.topic.schema.loading.enabled            | should the startup topic schema loading be enabled      | false
frontend.startup.topic.schema.loading.retry.count        | number of retries between topic schema fetch loops      | 5
frontend.startup.topic.schema.loading.thread.pool.size   | number of worker threads loading schemas concurrently   | 16
