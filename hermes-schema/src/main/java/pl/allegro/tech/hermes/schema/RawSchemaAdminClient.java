package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.TopicName;

public interface RawSchemaAdminClient extends RawSchemaClient {

    void registerSchema(TopicName topic, RawSchema rawSchema);

    void deleteAllSchemaVersions(TopicName topic);

    void validateSchema(TopicName topic, RawSchema rawSchema);
}
