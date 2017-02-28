# Publishing Avro

[Avro](<https://avro.apache.org/>) is a compact, binary data format widely used in BigData world. It is recommended that
all topics use this format, as it lowers volume of data sent to Kafka and is easy to analyze when dumping data from
Kafka to Hadoop.

## Conversion

Publisher can publish either JSON or Avro to Avro topic. This allows legacy systems, that might not have Avro support,
publish data to Hermes. Message content type is read from HTTP `Content-Type` header. If incoming message has
`application/json` content type, it will be treated as JSON and converted to Avro using specified schema.

## Validation

Each incoming message is validated against schema. Any message that fails to pass validation is dropped and publisher
receives *400 Bad Message* status with validation error details.

## Schema

Each Avro topic **must** have [Avro schema](http://avro.apache.org/docs/1.7.7/spec.html#schemas) defined.

Defining a good schema for complex data structures might be bit of a trial & error process, thus we prepared a tool
that helps you validate the schema locally before uploading it to Hermes.

### Create base schema

Create schema for your data by following [Avro schema spec](http://avro.apache.org/docs/1.7.7/spec.html#schemas).
Schema requires two special fields that identify it:

* **namespace**: set to Hermes **group name** by convention
* **name**: set to Hermes **topic name** by convention

Hermes appends metadata to each event, which has to be reflected in defined schema as well. Thus for each schema please
append the following field definition. **You should not modify published JSON**. `__metadata` field defaults to `null`.

```json
{
  "name": "__metadata",
  "type": [
    "null",
    {
      "type": "map",
      "values": "string"
    }
  ],
  "default": null,
  "doc": "Field used in Hermes internals to propagate metadata like hermes-id"
}
```

Example schema:

```json
{
  "namespace": "tech.hermes.group",
  "name": "topic",
  "type": "record",
  "doc": "This is a sample schema definition for some Hermes message",
  "fields": [
    {
      "name": "id",
      "type": "string",
      "doc": "Message id"
    },
    {
      "name": "content",
      "type": "string",
      "doc": "Message content"
    },
    {
      "name": "tags",
      "type": { "type": "array", "items": "string" },
      "doc": "Message tags"
    },
    {
      "name": "__metadata",
      "type": [
        "null",
        {
          "type": "map",
          "values": "string"
        }
      ],
      "default": null,
      "doc": "Field used in Hermes internals to propagate metadata like hermes-id"
    }
  ]
}
```

### Validating schema

To validate created schema use [Avro schema validator](https://github.com/allegro/json-avro-converter#validator). Ready-to-use
JAR file can be downloaded from [current release](https://github.com/allegro/json-avro-converter/releases).

```bash
java -jar validator/build/libs/json2avro-validator.jar -s sample-schema.avcs -i sample-message.json -m json2avro2json
```

This command will validate given message against the schema, convert the message from JSON to Avro and back from Avro
to JSON, so you can see if the formatters got everything right.

### Setting topic schema

Please refer to the [schema repository configuration](/configuration/schema-repository) section in order to use Avro schemas in Hermes.
Having up and running schema repository, send a POST request with topic's schema to Hermes management endpoint:

```bash
curl -X POST -H "Content-type: application/json" --data {schema} /topics/{topicName}/schema
```

## Schema versioning

A very important property of the Avro standard is schema backward compatibility. Each update to schema MUST
be backward compatible. There might be multiple versions of schema for given topic defined in
[schema repository](/configuration/schema-repository#schema-repo-store)).
Hermes always uses the *latest* schema version.

In case you need to break backwards compatibility, you should create new topic which defines new schema
and plan deprecation of old topic, including migrating subscribers from one topic to the other.

## Complex schema example

Writing a good Avro schema might be challenging for complex messages. Below is an example of schema that uses complex records, optionals, maps and arrays.

```json
{
    "namespace": "tech.hermes.group",
    "name": "topic",
    "type": "record",
    "doc": "This is a sample complex message schema",
    "fields": [
        {
            "name": "optionalField",
            "default": null,
            "type": [
                "null", {
                    "name": "optionalField_type",
                    "type": "record",
                    "fields": [
                        {
                            "name": "someField",
                            "type": "string"
                        }
                    ]
                }
            ]
        },
        {
            "name": "mapField",
            "type": "map",
            "values": "string"
        },
        {
            "name": "complexMapField",
            "type": "map",
            "values": {
                "name": "complexMapField_type",
                "type": "record",
                "fields": [
                    {
                        "name": "stringField",
                        "type": "string"
                    },
                    {
                        "name": "intField",
                        "type": "int"
                    }
                ]
            }
        },
        {
            "name": "arrayField",
            "type": "array",
            "values": "int"
        },
        {
            "name": "complexArrayField",
            "type": "array",
            "values": {
                "name": "complexArrayField_type",
                "type": "record",
                "fields": [
                    {
                        "name": "stringField",
                        "type": "string"
                    },
                    {
                        "name": "intField",
                        "type": "int"
                    }
                ]
            }
        }
    ]
}
```
