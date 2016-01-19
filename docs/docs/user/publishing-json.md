# Publishing JSON

When topic has content type set to **JSON** it will accept messages in JSON format and they will be stored in JSON
in Kafka.

## Schema

Topic owner can specify [JSON schema](<http://tools.ietf.org/html/draft-zyp-json-schema-04>) of messages sent to
given topic. Schema has purely informative function, unless owner decides to enable validation as well.

## Conversion

There are no conversion mechanisms for JSON topics.

## Validation

Validation is not enabled by default, as it might impact performance for complex messages. There is a separate metric
that measures time spent in validation phase, so owner can run performance tests before turning validation on.

Any message that fails to pass validation is dropped and publisher receives *400 Bad Message* status with validation
error details.
