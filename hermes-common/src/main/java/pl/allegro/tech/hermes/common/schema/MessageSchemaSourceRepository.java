package pl.allegro.tech.hermes.common.schema;

import pl.allegro.tech.hermes.api.Topic;

public interface MessageSchemaSourceRepository {

    String getSchemaSource(Topic topic);

}
