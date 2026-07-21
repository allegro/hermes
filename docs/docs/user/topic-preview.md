# Topic preview

> This is an optional feature, consult Hermes cluster administrator if it is switched on on your Hermes deployment.

When subscribing to a new topic it might be helpful to have an example of a published event, especially when there is no
topic schema attached. Hermes Frontend can gather a few messages from the topic and save them as a preview that can be
accessed via the Management module. In most cases it should be switched on only on test and dev environments.

Preview holds up to N messages per data center, where N is configured by the Hermes cluster administrator. It is a
limited sample, not a complete message log or a record of every message received during a time window. Each frontend
instance captures the first N messages it receives during its collection period. The latest persisted sample from one
frontend instance is retained in each data center, and Management aggregates those samples across data centers.

Messages for preview are saved on arrival in JSON format. For Avro, the message is converted to JSON and if it fails,
the raw byte array is saved. For JSON, no conversion is necessary. No other conversion is made on the message.

If preview is enabled, it is accessible in Hermes Console topic view or via Management REST API:

```
GET /topics/{topicName}/preview
```

Sample response:

```json
[
  {
    "content": "{\"hello\": \"world\"}",
    "truncated": false
  }
]
```
