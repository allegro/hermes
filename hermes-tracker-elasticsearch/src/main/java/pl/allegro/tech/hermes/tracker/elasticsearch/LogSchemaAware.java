package pl.allegro.tech.hermes.tracker.elasticsearch;

public interface LogSchemaAware {

  String MESSAGE_ID = "messageId";
  String BATCH_ID = "batchId";
  String TIMESTAMP = "timestamp";
  String TIMESTAMP_SECONDS = "timestamp_seconds";
  String PUBLISH_TIMESTAMP = "publish_timestamp";
  String STATUS = "status";
  String TOPIC_NAME = "topicName";
  String SUBSCRIPTION = "subscription";
  String PARTITION = "partition";
  String OFFSET = "offset";
  String REASON = "reason";
  String CLUSTER = "cluster";
  String SOURCE_HOSTNAME = "hostname";
  String REMOTE_HOSTNAME = "remote_hostname";
  String EXTRA_REQUEST_HEADERS = "extra_request_headers";
  String STORAGE_DATACENTER = "storageDc";
}
