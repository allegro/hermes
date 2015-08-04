package pl.allegro.tech.hermes.common.schema;

import pl.allegro.tech.hermes.api.Topic;

public interface MessageSchemaSourceProvider {

    String get(Topic topic);

}
