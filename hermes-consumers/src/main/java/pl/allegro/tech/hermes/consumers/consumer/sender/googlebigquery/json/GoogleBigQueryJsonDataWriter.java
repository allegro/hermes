package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json;

import com.google.api.core.ApiFuture;
import com.google.cloud.bigquery.storage.v1.AppendRowsResponse;
import com.google.cloud.bigquery.storage.v1.JsonStreamWriter;
import com.google.protobuf.Descriptors;
import java.io.IOException;
import org.json.JSONArray;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.GoogleBigQueryDataWriter;

public class GoogleBigQueryJsonDataWriter
    extends GoogleBigQueryDataWriter<
        JSONArray, JsonStreamWriter, GoogleBigQueryJsonStreamWriterFactory> {

  public GoogleBigQueryJsonDataWriter(
      String streamName, GoogleBigQueryJsonStreamWriterFactory factory) {
    super(streamName, factory);
  }

  @Override
  protected ApiFuture<AppendRowsResponse> append(JSONArray message)
      throws Descriptors.DescriptorValidationException, IOException {
    return streamWriter.append(message);
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
