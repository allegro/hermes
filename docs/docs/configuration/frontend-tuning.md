# Tuning Frontend

Frontend module is crucial for the client-perceived performance and stability of Hermes. There are many configuration
options that can be changed to fine tune the [Undertow HTTP server](http://undertow.io) and communication with Kafka.

## HTTP Server

Hermes Frontend can be configured to send the response after no longer than specific timeout. This prevents long-running
requests in case of Kafka downtime and helps to reason about service SLA. This is related to
[response codes](../user/publishing.md#response-codes): depending on whether message has been persisted in Kafka before the
timeout, publisher receives **201** or **202** response code.

You can change the timeout values separately for messages with different [acknowledgment levels](../user/publishing.md#acknowledgment-level).
To do so, use

* `frontend.handlers.idleTimeout` for leader ACK
* `frontend.handlers.longIdleTimeout` for all ACK

Those timeouts are counted from the time the request has been parsed till response is sent. This means, that
interaction with Kafka needs to take place during this period. If timeout is reached when event is in
*sending to Kafka* state, **202 Accepted** response is returned, since message is already in sending buffer and Hermes
guarantees it will end up in Kafka. Otherwise, normal *request timed out* message is sent.


Option                                      | Description                                                                          | Default value
------------------------------------------- | ------------------------------------------------------------------------------------ | -------------
frontend.server.port                        | port to listen on                                                                    | 8080
frontend.server.readTimeout                 | maximum time to wait for first portion of data to appear on socket                   | 2s
frontend.server.requestParseTimeout         | maximum time to wait before full request is received                                 | 5s
frontend.server.ioThreadsCount              | number of Undertow IO threads                                                        | 2 * cores
frontend.server.workerThreadCount           | number of Undertow worker threads                                                    | 200
frontend.server.gracefulShutdownInitialWait | time between setting health endpoint to return DOWN and actually stopping the server | 10s

Default timeout settings make Frontend safe against [Slowloris attack](https://en.wikipedia.org/wiki/Slowloris_(software)).


## Buffers

Hermes uses Kafka in-memory send buffers to tolerate any downtime or hiccups, as described in
[publishing guide](../user/publishing.md#buffering). Use `frontend.messages.local.storage.bufferedSizeBytes` option to change the size of buffer.
By default it is set to **256 MB**. Changing the size might extend the period for which Hermes is able to receive
messages in case of Kafka downtime. Since internally there are two Kafka producers spawned, one for ACK-leader and one
for ACK-all, there are also **two buffers** - keep this in mind when deciding on heap size.

## Kafka

Kafka producer properties map 1:1 to Kafka producer configuration options. See Kafka documentation if you have any doubts
or need an extended description.

Option                                    | Kafka config            | Description                            | Default value
----------------------------------------- | ----------------------- | -------------------------------------- | -------------
frontend.kafka.producer.metadataMaxAge    | METADATA_MAX_AGE_CONFIG | how old can topic metadata be          | 5m
frontend.kafka.producer.compressionCodec  | COMPRESSION_TYPE_CONFIG | compression algorithm                  | none
frontend.kafka.producer.retries           | RETRIES_CONFIG          | how many times should we retry sending | Integer.MAX_VALUE
frontend.kafka.producer.retryBackoff      | RETRY_BACKOFF_MS_CONFIG | backoff between retries                | 256ms
frontend.kafka.producer.batchSize         | BATCH_SIZE_CONFIG       | size of sent message batch in bytes    | 16 kB
frontend.kafka.producer.tcpSendBuffer     | SEND_BUFFER_CONFIG      | size of TCP buffer                     | 128 kB

## Graceful startup

Processing time for first event arriving at each topic may be longer than expected from Hermes frontend.
This is because topic metadata has to be fetched from Kafka. What is more, Avro topics need schema in order to validate incoming messages.

To get rid of this issue and reduce event processing latency, all the required data can be fetched during Hermes frontends startup,
just before it's health-check goes green.
Note, enabling startup data loading will make frontend boot a little longer.

Option                                                   | Description                                             | Default value
-------------------------------------------------------- | ------------------------------------------------------- | -------------
frontend.startup.topic.loading.metadata.enabled          | should the startup topic metadata loading be enabled    | false
frontend.startup.topic.loading.metadata.retryInterval    | retry interval between retry loops                      | 1s
frontend.startup.topic.loading.metadata.retryCount       | number of retries between topic metadata fetch loops    | 5
frontend.startup.topic.loading.metadata.threadPoolSize   | number of worker threads loading metadata concurrently  | 16
frontend.startup.topic.loading.schema.enabled            | should the startup topic schema loading be enabled      | false
frontend.startup.topic.loading.schema.retryCount         | number of retries between topic schema fetch loops      | 3
frontend.startup.topic.loading.schema.threadPoolSize     | number of worker threads loading schemas concurrently   | 16
