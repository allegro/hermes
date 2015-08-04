package pl.allegro.tech.hermes.management.domain.topic.schema;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.schema.MessageSchemaSourceProvider;

public interface MessageSchemaSourceRepository extends MessageSchemaSourceProvider {

    void save(String schemaSource, Topic topic);

    void delete(Topic topic);

}
