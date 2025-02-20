package pl.allegro.tech.hermes.tracker.elasticsearch;

import java.util.concurrent.Callable;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentBuilder;

public class ElasticsearchDocument {

  private final BytesReference bytesReference;

  private ElasticsearchDocument(BytesReference bytesReference) {
    this.bytesReference = bytesReference;
  }

  public BytesReference bytes() {
    return bytesReference;
  }

  public static ElasticsearchDocument build(Callable<XContentBuilder> builder) {
    try {
      return new ElasticsearchDocument(BytesReference.bytes(builder.call()));
    } catch (Exception e) {
      throw new ElasticsearchRepositoryException(e);
    }
  }
}
