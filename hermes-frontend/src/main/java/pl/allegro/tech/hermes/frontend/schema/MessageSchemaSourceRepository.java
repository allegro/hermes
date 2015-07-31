package pl.allegro.tech.hermes.frontend.schema;

import pl.allegro.tech.hermes.api.Topic;

public interface MessageSchemaSourceRepository {

    String getSchemaSource(Topic topic);

}
