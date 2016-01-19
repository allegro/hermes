# Tuning Consumers

## HTTP Sender

Option                                               | Description                                                 | Default value
---------------------------------------------------- | ----------------------------------------------------------- | -------------
consumer.http.client.request.timeout                 | how much time we wait for client response before timing out | 1000ms
consumer.http.client.thread.pool.size                | size of thread pool for sender threads (global)             | 30
consumer.http.client.max.connections.per.destination | max connections per remote host                             | 100

## Consumers core

Option                        | Description                                                              | Default value
----------------------------- | ------------------------------------------------------------------------ | -------------
consumer.commit.offset.period | interval between committing offsets to Kafka                             | 20s
consumer.thread.pool.size     | thread pool for threads involved in consuming, 1 thread per subscription | 500
consumer.inflight.size        | how many messages can be kept in send queue, per subscription            | 100
