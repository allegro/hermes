package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;

public interface SchemaSourceProvider {

    SchemaSource get(Topic topic);

}
