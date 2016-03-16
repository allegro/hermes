package pl.allegro.tech.hermes.domain.topic.schema;

import com.google.common.base.Strings;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Simple implementation not requiring any infrastructure for keeping schemas.
 * Pretends to support schema versioning by always returning latest schema.
 */
public class TopicFieldSchemaSourceProvider implements SchemaSourceProvider {

    @Override
    public Optional<SchemaSource> get(Topic topic) {
        return Optional.ofNullable(topic.getMessageSchema()).map(Strings::emptyToNull).map(SchemaSource::valueOf);
    }

    @Override
    public Optional<SchemaSource> get(Topic topic, SchemaVersion version) {
        return get(topic);
    }

    @Override
    public List<SchemaVersion> versions(Topic topic) {
        return Collections.singletonList(SchemaVersion.valueOf(0));
    }
}
