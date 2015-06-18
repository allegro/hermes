package pl.allegro.tech.hermes.tracker.elasticsearch;

import org.elasticsearch.common.xcontent.XContentBuilder;

import java.util.concurrent.Callable;

public class DocumentBuilder {

    public static XContentBuilder build(Callable<XContentBuilder> builder) {
        try {
            return builder.call();
        } catch (Exception e) {
            throw new ElasticsearchRepositoryException(e);
        }
    }
}
