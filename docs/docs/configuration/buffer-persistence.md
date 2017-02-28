# Publishing buffer persistence

Hermes Frontend API has option to register callbacks triggered during different phases of message lifetime:

* BrokerAcknowledgedListener: message has been acknowledged by broker, can be considered as persisted
* BrokerTimeoutListener: broker did not save message in time, it is now stored in memory buffer and retried until
    successfull
* BrokerErrorListener: there was some kind of error (e.g. no connection to broker) when trying to send message to broker

## ChronicleMap implementation

Default implementation uses [OpenHFT ChronicleMap](https://github.com/OpenHFT/Chronicle-Map) to persist unsent messages
to disk. Map structure is continuously persisted to disk, as it is stored in offheap memory as
[memory mapped file](https://en.wikipedia.org/wiki/Memory-mapped_file).

When Hermes Frontend starts up it scans filesystem in search of existing persisted map. If found, it is read and any
persisted events are sent to Message Store. This way recovering after crash is fully automatic. If Hermes process or
server crashes, nothing is lost.

There is additional protection against flooding subscribers with outdated events. When reading events from persisted
storage, Hermes filters out messages older than N hours, where N is a system parameter and is set to 3 days by default.
This might be useful when reviving Frontend nodes that have been down for a longer period of time.

Option                                          | Description                                            | Default value
----------------------------------------------- | ------------------------------------------------------ | --------------
frontend.messages.local.storage.enabled         | enable persistent buffer                               | false
frontend.messages.local.storage.max.age.hours   | ignore messages in buffer that are older than N hours  | 72
frontend.messages.local.storage.directory       | location of memory mapped files                        | /tmp/<tmp dir>

### Buffer files

Buffer is persisted into `hermes-buffer.dat` file in storage directory. On startup, if previous persistence file exists,
it is renamed to `hermes-buffer-<timestamp>.dat`. This is a temporary file, deleted after all messages are
read and sent to Kafka.

## Custom implementation

To register callbacks use methods exposed in `HermesFrontend.Builder`:

```java
class BrokerListener implements BrokerAcknowledgedListener,
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

class HermesStarter {

    public void start(BrokerListener listener) {
        HermesFrontend frontend = HermesFrontend.frontend()
            .withBrokerAcknowledgeListener(brokerListener)
            .withBrokerTimeoutListener(brokerListener)
            .withBrokerErrorListener(brokerListener)
            .build();
        frontend.start();
    }
}
```
