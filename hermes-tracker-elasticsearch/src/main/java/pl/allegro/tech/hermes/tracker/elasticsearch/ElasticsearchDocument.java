package pl.allegro.tech.hermes.tracker.elasticsearch;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.util.concurrent.Callable;

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
            return new ElasticsearchDocument(builder.call().bytes());
        } catch (Exception e) {
            throw new ElasticsearchRepositoryException(e);
        }
    }
}
