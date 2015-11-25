Migration from JSON to AVRO
===========================

This guide explains how to change data format of an existing JSON topic to `Apache AVRO <https://avro.apache.org/>`_.

Pros of migrating topics to AVRO
----
* lower disk & bandwith usage
* all handled events are validated against provided schema
* still both AVRO & JSON messages are accepted on the migrated topic. Hermes converts all JSON message to AVRO on the fly.

Prerequisite
------------
Prepare `AVRO schema <https://avro.apache.org/docs/current/spec.html#schemas>`_ which will define the data structure of published events for the migrated topic.
Add ``__metadata`` field to the schema as follows::

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

The field is **required**. Hermes will use it for its internal purposes (like passing ``Hermes-Message-Id``).

Migration
---------

1. Add AVRO schema
^^^^^^^^^^^^^^^^^^

Send request with a valid AVRO schema to hermes-management on endpoint::

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


``validate=false`` means that schema validation is disabled. Right now we are working with topic of JSON type, so schema
would be validated as JSON schema. We don't want that because JSON & AVRO schema have different structures.

2. Enabling dry run mode
^^^^^^^^^^^^^^^^^^^^^^^^

This step is optional. Can be done to verify if published events in JSON format convert properly to AVRO.
To enable the dry run mode send::

    PUT /topics/{topicName}

    {"jsonToAvroDryRunEnabled": true}

When dry run mode is enabled, try publishing some JSON messages on the migrated topic.
If there are no logs of type ``Could not convert JSON to AVRO`` then it means that messages are converted successfully and you can disable the dry run mode.

3. Enabling migration mode
^^^^^^^^^^^^^^^^^^^^^^^^^^
To change topic type from JSON to AVRO send::

    PUT /topics/{topicName}

    {"migratedFromJsonType": true, "contentType": "AVRO"}


This will create additional ``{topicName}_avro`` topic in Kafka.
From now on all events published on ``{topicName}`` will be converted to AVRO and stored in ``{topicName}_avro``.
Hermes will send all JSON messages stored in old kafka topic ``{topicName}`` as well.

Rollback
^^^^^^^^
If something goes wrong in one of the previous steps or you simply want to change the topic type back to JSON,
you have to manually modify topic's ``{hermes_root}/groups/{group}/topics/{topic}`` Zookeeper node by:

* disabling ``validation`` & ``migratedFromJsonType`` flags
* setting ``contentType`` back to ``JSON``.
