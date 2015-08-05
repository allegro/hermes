package pl.allegro.tech.hermes.management.domain.topic.schema;

import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaSourceProvider;

public interface SchemaSourceRepository extends SchemaSourceProvider {

    void save(SchemaSource schemaSource, Topic topic);

    void delete(Topic topic);

}
