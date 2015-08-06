package pl.allegro.tech.hermes.domain.topic.schema;

import com.google.common.base.Strings;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;

import java.util.Optional;

public class TopicFieldSchemaSourceProvider implements SchemaSourceProvider {

    @Override
    public Optional<SchemaSource> get(Topic topic) {
        return Optional.ofNullable(topic.getMessageSchema()).map(Strings::emptyToNull).map(SchemaSource::valueOf);
    }

}
