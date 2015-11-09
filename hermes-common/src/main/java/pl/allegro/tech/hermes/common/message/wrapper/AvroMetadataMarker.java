package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.util.Utf8;

public interface AvroMetadataMarker {
    String METADATA_MARKER = "__metadata";
    Utf8 METADATA_TIMESTAMP_KEY = new Utf8("timestamp");
    Utf8 METADATA_MESSAGE_ID_KEY = new Utf8("messageId");
    Utf8 METADATA_TRACE_ID_KEY = new Utf8("traceId");
    Utf8 METADATA_SPAN_ID_KEY = new Utf8("spanId");
    Utf8 METADATA_PARENT_SPAN_ID_KEY = new Utf8("parentSpanId");
    Utf8 METADATA_TRACE_SAMPLED_KEY = new Utf8("traceSampled");
    Utf8 METADATA_TRACE_REPORTED_KEY = new Utf8("traceReported");
}
