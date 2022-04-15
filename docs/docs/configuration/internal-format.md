# Message internal format

Hermes appends internal metadata to every published message. They are not visible to neither publisher nor subscriber,
but are present on Kafka. Thus if you use tools to read data directly from Kafka, you will see events along with their
metadata. Metadata format depends on data format (Avro, JSON) - please see format documentation for details.

Currently metadata holds two fields:

* **messageId** - string containing Hermes-Message-Id as UUID v4
* **timestamp** - Unix epoch timestamp in milliseconds holding date of reception

There is no guarantee that this metadata won't be extended.

## JSON

Messages with metadata have following format:

```json
{
    "metadata": {
        "messageId": "b0838e5f-c3d1-47b3-8949-abe7a1f65202",
        "timestamp": 1439129947000
    },
    "message": {
        // contents of your message goes here
    }
}
```

## Avro

Hermes metadata is stored in `__metadata` field, which is specified as map of optional elements. It will always contain at least
``messageId`` and ``timestamp``.

```json
{
  "type": "record",
  "name": "SomeTopic",
  "namespace": "pl.allegro.hermes",
  "doc": "Schema of some event",
  "fields": [
    {
      "name": "__metadata",
      "type": [
        "null",
        {
          "type": "map",
          "values": "string"
        }
      ],
      "doc": "Field used to propagate metadata like messageId and timestamp",
      "default": null
    },
    // other event fields
  ]
}
```

## Custom reading internal messages

Hermes allows to provide custom implementation of reading Kafka records, for example for reading metadata from Kafka headers.

To do this, implement the interfaces `MessageContentReader` and `MessageContentReaderFactory`:

```java
class CustomMessageContentReader implements MessageContentReader {
    @Override
    public UnwrappedMessageContent read(ConsumerRecord<byte[], byte[]> message, ContentType contentType) {
        // custom implementation of reading consumer record
    }
}

class CustomMessageContentReaderFactory implements MessageContentReaderFactory {
    @Override
    public MessageContentReader provide(Topic topic) {
        return new CustomMessageContentReader();
    }
}
```

and register the implementation of `MessageContentReaderFactory` as a bean:

```java
@Configuration
public class CustomHermesConsumersConfiguration {

    @Bean
    @Primary
    public MessageContentReaderFactory customMessageContentReaderFactory() {
        return new CustomMessageContentReaderFactory();
    }
}
```
