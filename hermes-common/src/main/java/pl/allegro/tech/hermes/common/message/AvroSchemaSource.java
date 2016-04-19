package pl.allegro.tech.hermes.common.message;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.CompiledSchema;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaMissingException;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaVersion;

import java.util.List;
import java.util.function.Function;

public interface AvroSchemaSource {
    CompiledSchema<Schema> getAvroSchema(Topic topic, SchemaVersion version);
    List<SchemaVersion> versions(Topic topic, boolean online);


    Logger logger = LoggerFactory.getLogger(AvroSchemaSource.class);

    default List<SchemaVersion> versions(Topic topic) {
        return versions(topic, false);
    }

    default <T> T invokeWithFallback(Topic topic, Function<CompiledSchema<Schema>, T> processor, List<SchemaVersion> availableVersions) {
        for (SchemaVersion version : availableVersions) {
            try {
                return processor.apply(getAvroSchema(topic, version));
            } catch (Exception ex) {
                logger.debug("Failed to match schema for message for topic {}, schema version {}, fallback to previous.", topic.getQualifiedName(), version.value());
            }
        }
        logger.error("Could not match schema from cache for message for topic {} {}", topic.getQualifiedName(), SchemaVersion.toString(availableVersions));
        throw new SchemaMissingException(topic);
    }

    // try-harding to find proper schema
    default <T> T tryHard(Topic topic, Function<CompiledSchema<Schema>, T> processor) {
        try {
            return invokeWithFallback(topic, processor, versions(topic));
        } catch (Exception ex) {
            logger.info("Trying to find schema online for message for topic {}", topic.getQualifiedName());
            return invokeWithFallback(topic, processor, versions(topic, true));
        }
    }

}
