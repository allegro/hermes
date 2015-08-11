package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;

import java.util.Optional;

public interface SchemaSourceProvider {

    Optional<SchemaSource> get(Topic topic);

}
