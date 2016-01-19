# Publishing Avro [*incubating*]

[Avro](<https://avro.apache.org/>) is a compact, binary data format widely used in BigData world. It is recommended that
all topics use this format, as it lowers volume of data sent to Kafka and is easy to analyze when dumping data from
Kafka to Hadoop.

## Schema

Each Avro topic **must** have [Avro schema](https://avro.apache.org/docs/1.7.7/spec.html#Protocol+Wire+Format) defined.
When adding Avro schema via Hermes Management, it is enriched to reflect the internal format of data, so you can safely
use it do describe your data on e.x. Hadoop.

## Conversion

Publisher can publish either JSON or Avro to Avro topic. This allows legacy systems, that might not have Avro support,
publish data to Hermes. Message content type is read from HTTP `Content-Type` header. If incoming message has
`application/json` content type, it will be treated as JSON and converted to Avro using specified schema.

## Validation

Each incoming message is validated against schema. Any message that fails to pass validation is dropped and publisher
receives *400 Bad Message* status with validation error details.
