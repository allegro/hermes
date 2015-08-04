package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.Topic;

public interface SchemaSourceProvider {

    String get(Topic topic);

}
