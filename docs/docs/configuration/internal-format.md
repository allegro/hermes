# Message internal format

Hermes appends internal metadata to every published message. They are not visible to neither publisher nor subscriber,
but are present on Kafka. Thus if you use tools to read data directly from Kafka, you will see events along with their
metadata. Metadata format depends on data format (Avro, JSON) - please see format documentation for details.

Currently metadata holds two fields:

* **id** - string containing Hermes-Message-Id as UUID v4
* **timestamp** - Unix epoch timestamp in milliseconds holding date of reception

There is no guarantee that this metadata won't be extended.

## JSON

Messages with metadata have following format:

```json
{
    "metadata": {
        "id": "b0838e5f-c3d1-47b3-8949-abe7a1f65202",
        "timestamp": 1439129947000
    },
    "message": {
        // contents of your message goes here
    }
}
```

## Avro *[incubating]*

Hermes metadata is stored in `__metadata` field, which is specified as map of optional elements. It will always contain
``id`` and ``timestamp``.
