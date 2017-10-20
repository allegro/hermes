# Topic preview

> This is an optional feature, consult Hermes cluster administrator if it is switched on on your Hermes deployment.

When subscribing to new topic it might be helpful to have an example of published event, especially when there is no
topic schema attached. This is why Hermes Frontend has option of gathering a few messages from the topic and saving them
as a preview that can be accessed via Management module. In most cases it should be switched on only on test and dev
environments.

Preview holds up to N messages, where N is configured by Hermes cluster administrator. Messages are refreshed and should
represent the current format of messages on given topic. Messages for preview are saved on arrival in JSON format. 
For Avro, the message is converted to JSON and if it fails, raw byte array is saved. For JSON format no conversion 
is necessary. No other conversion is made on the message.

If preview is enabled, it is accessible in Hermes Console topic view or via Management REST API:

```
GET /topics/{topicName}/preview
```

Sample response:

```json
[
    "{'hello': 'world'}"
]
```
