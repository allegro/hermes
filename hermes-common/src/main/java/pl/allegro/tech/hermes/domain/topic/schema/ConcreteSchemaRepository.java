package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.Topic;

public interface ConcreteSchemaRepository<T> {

    VersionedSchema<T> getSchema(Topic topic, int version);

}
