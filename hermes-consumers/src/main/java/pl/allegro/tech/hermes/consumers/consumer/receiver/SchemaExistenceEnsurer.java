package pl.allegro.tech.hermes.consumers.consumer.receiver;

import java.time.Duration;
import java.util.function.Supplier;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.SchemaOnlineChecksRateLimiter;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaException;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersion;


public class SchemaExistenceEnsurer {
    private static final Logger logger = LoggerFactory.getLogger(SchemaExistenceEnsurer.class);
    private static final boolean REFRESH_ONLINE = true;

    private final SchemaRepository schemaRepository;
    private final RetryPolicy<SchemaPullStatus> retryPolicy;
    private final SchemaOnlineChecksRateLimiter rateLimiter;

    public SchemaExistenceEnsurer(SchemaRepository schemaRepository, Duration waitSchemaInterval,
                                  SchemaOnlineChecksRateLimiter rateLimiter) {
        this.schemaRepository = schemaRepository;
        this.retryPolicy = new RetryPolicy<SchemaPullStatus>()
                .withDelay(waitSchemaInterval)
                .withMaxRetries(Integer.MAX_VALUE)
                .handleIf((schemaPullStatus, throwable) -> schemaPullStatus != SchemaPullStatus.PULLED);
        this.rateLimiter = rateLimiter;
    }

    public void ensureSchemaExists(Topic topic, SchemaVersion version) {
        Failsafe.with(retryPolicy).get(() -> pullSchemaIfNeeded(topic, version));
    }

    public void ensureSchemaExists(Topic topic, SchemaId id) {
        Failsafe.with(retryPolicy).get(() -> pullSchemaIfNeeded(topic, id));
    }

    private SchemaPullStatus pullSchemaIfNeeded(Topic topic, SchemaVersion version) {
        String msg = String.format("Could not find schema version [%s] provided in header for topic [%s]." +
                " Pulling schema online...", version, topic);
        return pullSchemaIfNeeded(topic, () -> schemaRepository.getAvroSchema(topic, version), msg);
    }

    private SchemaPullStatus pullSchemaIfNeeded(Topic topic, SchemaId id) {
        String msg = String.format("Could not find schema id [%s] provided in header for topic [%s]." +
                " Pulling schema online...", id, topic);
        return pullSchemaIfNeeded(topic, () -> schemaRepository.getAvroSchema(topic, id), msg);
    }

    private SchemaPullStatus pullSchemaIfNeeded(Topic topic, Supplier<CompiledSchema<Schema>> schemaProvider,
                                                String errorMessage) {
        if (!rateLimiter.tryAcquireOnlineCheckPermit()) {
            return SchemaPullStatus.NOT_PULLED;
        }
        try {
            schemaProvider.get();
            return SchemaPullStatus.PULLED;
        } catch (SchemaException ex) {
            logger.warn(errorMessage, ex);
            pullOnline(topic);
            return SchemaPullStatus.NOT_PULLED;
        }
    }

    private void pullOnline(Topic topic) {
        schemaRepository.getVersions(topic, REFRESH_ONLINE);
    }

    enum SchemaPullStatus {
        PULLED,
        NOT_PULLED
    }
}
