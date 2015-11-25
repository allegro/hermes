Migration from JSON to AVRO
===========================

The guide explains how to switch data format from JSON to AVRO for existing topic.

Pros
----
* lower disk & bandwith usage
* all handled events are valid with schema
* AVRO & JSON can be published on migrated topic. Hermes converts JSON -> AVRO in flight.

Prerequisite
------------
Prepare AVRO schema which defines structure of published events for migrated topic.
Add to the schema following field::

    {
        "name": "__metadata",
        "type": [
            "null",
            {
                "type": "map",
                "values": "string"
            }
        ],
        "default": null
    }

The field is required. Hermes use it in internals to propagate metadata like `Hermes-Message-Id`.

Migration
---------

1. Add AVRO schema
^^^^^^^^^^^^^^^^^^

Send request with valid AVRO schema to hermes-management on endpoint::

    POST /topics/{topicName}/schema?validate=false

    {
        "namespace": "org.example",
        "name": "User",
        "type": "record",
        "fields": [
            {
                "name": "name",
                "type": "string",
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
                "default": null
            }
        ]
    }


`validate=false` means that schema validation is disabled. Right now we are working with topic type of JSON, so schema
would be validated as JSON schema. We don't want that because JSON & AVRO schema have different structures.

2. Enabling dry run mode
^^^^^^^^^^^^^^^^^^^^^^^^

Optional step. Can be done to verify if published events in json format converts properly to AVRO.
To enable dry run mode send::

    PUT /topics/{topicName}

    {"jsonToAvroDryRunEnabled": true}

After dry run mode is enabled, try to publish json messages on migrated topic. If there are no logs type of
`Could not convert JSON to AVRO` then it means that messages are converted successfully and you can disable dry run mode analogously.

3. Enabling migration mode
^^^^^^^^^^^^^^^^^^^^^^^^^^
To change topic type from JSON to AVRO send::

    PUT /topics/{topicName}

    {"migratedFromJsonType": true, "contentType": "AVRO"}


After this request additional topic with name `{topicName}_avro` will be created in Kafka.
All events published on `{topicName}` will be converted to avro and stored in `{topicName}_avro`.
Hermes also sends all json messages from old kafka topic {topicName}.

Rollback
^^^^^^^^
If something goes wrong in step number 4. or for some reason would you like to go back to JSON topic then you need to change
topic structure in proper zookeeper node - disable `validation` & `migratedFromJsonType` flags and change
value of `contentType` to `JSON`.