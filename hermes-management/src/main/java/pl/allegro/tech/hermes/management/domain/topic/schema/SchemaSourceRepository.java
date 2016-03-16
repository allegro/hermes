package pl.allegro.tech.hermes.management.domain.topic.schema;

import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaSourceProvider;

public interface SchemaSourceRepository extends SchemaSourceProvider {

    void save(SchemaSource schemaSource, Topic topic);
    default void save(SchemaSource schemaSource, Topic topic, int version) {
        throw new UnsupportedOperationException("sry");
    }

    void delete(Topic topic);
    default void delete(Topic topic, int version) {
        throw new UnsupportedOperationException("sry");
    }
}
