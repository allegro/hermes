package pl.allegro.tech.hermes.consumers.consumer.receiver;

import java.time.Duration;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.schema.SchemaException;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersion;


public class SchemaExistenceEnsurer {
    private static final Logger logger = LoggerFactory.getLogger(SchemaExistenceEnsurer.class);
    private final SchemaRepository schemaRepository;
    private final RetryPolicy<SchemaPullStatus> retryPolicy;

    public SchemaExistenceEnsurer(SchemaRepository schemaRepository, Duration waitSchemaInterval) {
        this.schemaRepository = schemaRepository;
        this.retryPolicy = new RetryPolicy<SchemaPullStatus>()
                .withDelay(waitSchemaInterval)
                .withMaxRetries(Integer.MAX_VALUE)
                .handleIf((schemaPullStatus, throwable) -> schemaPullStatus != SchemaPullStatus.PULLED);
    }

    public void ensureSchemaExists(Topic topic, int version) {
        Failsafe.with(retryPolicy).get(() -> pullSchemaIfNeeded(topic, version));
    }

    private SchemaPullStatus pullSchemaIfNeeded(Topic topic, int version) {
        try {
            schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(version));
            return SchemaPullStatus.PULLED;
        } catch (SchemaException ex) {
            logger.warn("Could not find schema version [{}] provided in header [{}] for topic [{}]." +
                    " Pulling schema online...", version, topic, ex);
            pullOnline(topic); // no need to handle an exception, the method never throws any
            return SchemaPullStatus.NOT_PULLED;
        }
    }

    private void pullOnline(Topic topic) {
        schemaRepository.getVersions(topic, true);
    }

    enum SchemaPullStatus {
        PULLED,
        NOT_PULLED
    }
}
