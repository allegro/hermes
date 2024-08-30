package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.util.Utf8;

public interface AvroMetadataMarker {
  String METADATA_MARKER = "__metadata";
  Utf8 METADATA_TIMESTAMP_KEY = new Utf8("timestamp");
  Utf8 METADATA_MESSAGE_ID_KEY = new Utf8("messageId");
}
