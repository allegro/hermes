# Publishing buffer persistence [deprecated]

Hermes Frontend API has option to register callbacks triggered during different phases of message lifetime:

* BrokerAcknowledgedListener: message has been acknowledged by broker, can be considered as persisted
* BrokerTimeoutListener: broker did not save message in time, it is now stored in memory buffer and retried until
  successful
* BrokerErrorListener: there was some kind of error (e.g. no connection to broker) when trying to send message to broker

## ChronicleMap implementation

Default implementation uses [OpenHFT ChronicleMap](https://github.com/OpenHFT/Chronicle-Map) to persist unsent messages
to disk. Map structure is continuously persisted to disk, as it is stored in offheap memory as
[memory mapped file](https://en.wikipedia.org/wiki/Memory-mapped_file).

When Hermes Frontend starts up it scans filesystem in search of existing persisted map. If found, it is read and any
persisted events are sent to Message Store. This way recovering after crash is fully automatic. If Hermes process or
server crashes, events that were flushed to disk are recovered. 

There is additional protection against flooding subscribers with outdated events. When reading events from persisted
storage, Hermes filters out messages older than N hours, where N is a system parameter and is set to 3 days by default.
This might be useful when reviving Frontend nodes that have been down for a longer period of time.

Option                                          | Description                                            | Default value
----------------------------------------------- | ------------------------------------------------------ | --------------
frontend.messages.local.storage.enabled         | enable persistent buffer                               | false
frontend.messages.local.storage.maxAge          | ignore messages in buffer that are older than N hours  | 72h
frontend.messages.local.storage.directory       | location of memory mapped files                        | /tmp/<tmp dir>

### Buffer files

Buffer is persisted into `hermes-buffer.dat` file in storage directory. On startup, if previous persistence file exists,
it is renamed to `hermes-buffer-<timestamp>.dat`. This is a temporary file, deleted after all messages are read and sent
to Kafka.

## Custom implementation

To register custom callbacks register the implementations as beans:

```java
class MyCustomBrokerListener implements BrokerAcknowledgedListener,
        BrokerTimeoutListener,
        BrokerErrorListener {

    @Override
    public void onAcknowledge(Message message, Topic topic) {
        /* ... */
    }

    @Override
    public void onTimeout(Message message, Topic topic) {
        /* ... */
    }

    @Override
    public void onError(Message message, Topic topic, Exception ex) {
        /* ... */
    }
}
```

```java
@Configuration
public class CustomHermesFrontendConfiguration {

    @Primary
    @Bean
    public BrokerListeners myBrokerListeners() {
        BrokerListener customBrokerListener = new MyCustomBrokerListener();
        BrokerListeners brokerListeners = new BrokerListeners();

        brokerListeners.addAcknowledgeListener(customBrokerListener);
        brokerListeners.addTimeoutListener(customBrokerListener);
        brokerListeners.addErrorListener(customBrokerListener);

        return brokerListeners;
    }
}
```
