package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro;

import com.google.api.core.ApiFuture;
import com.google.cloud.bigquery.storage.v1.AppendRowsResponse;
import com.google.cloud.bigquery.storage.v1.SchemaAwareStreamWriter;
import com.google.protobuf.Descriptors;
import java.io.IOException;
import java.util.Collections;
import org.apache.avro.generic.GenericRecord;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.GoogleBigQueryDataWriter;

public class GoogleBigQueryAvroDataWriter
    extends GoogleBigQueryDataWriter<
        GenericRecord,
        SchemaAwareStreamWriter<GenericRecord>,
        GoogleBigQueryAvroStreamWriterFactory> {

  public GoogleBigQueryAvroDataWriter(
      String streamName, GoogleBigQueryAvroStreamWriterFactory factory) {
    super(streamName, factory);
  }

  @Override
  protected ApiFuture<AppendRowsResponse> append(GenericRecord message)
      throws Descriptors.DescriptorValidationException, IOException {
    return streamWriter.append(Collections.singletonList(message));
  }

  @Override
  protected String getWriterId() {
    return streamWriter.getWriterId();
  }

  @Override
  protected String getStreamName() {
    return streamWriter.getStreamName();
  }
}
