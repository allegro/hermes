package pl.allegro.tech.hermes.consumers.consumer.receiver;

import java.time.Duration;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.SchemaOnlineChecksRateLimiter;
import pl.allegro.tech.hermes.schema.SchemaException;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersion;


public class SchemaExistenceEnsurer {
    private static final Logger logger = LoggerFactory.getLogger(SchemaExistenceEnsurer.class);

    private final SchemaRepository schemaRepository;
    private final RetryPolicy<SchemaPullStatus> retryPolicy;
    private final SchemaOnlineChecksRateLimiter rateLimiter;

    public SchemaExistenceEnsurer(SchemaRepository schemaRepository, Duration waitSchemaInterval, SchemaOnlineChecksRateLimiter rateLimiter) {
        this.schemaRepository = schemaRepository;
        this.retryPolicy = new RetryPolicy<SchemaPullStatus>().withDelay(waitSchemaInterval).withMaxRetries(Integer.MAX_VALUE).handleIf((schemaPullStatus, throwable) -> schemaPullStatus != SchemaPullStatus.PULLED);
        this.rateLimiter = rateLimiter;
    }

    public void ensureSchemaExists(Topic topic, SchemaVersion version) {
        Failsafe.with(retryPolicy).get(() -> pullSchemaIfNeeded(topic, version));
    }

    public void ensureSchemaExists(Topic topic, SchemaId id) {
        Failsafe.with(retryPolicy).get(() -> pullSchemaIfNeeded(topic, id));
    }

    private SchemaPullStatus pullSchemaIfNeeded(Topic topic, SchemaVersion version) {
        if (!rateLimiter.tryAcquireOnlineCheckPermit()) {
            return SchemaPullStatus.NOT_PULLED;
        }
        try {
            schemaRepository.getAvroSchema(topic, version);
            return SchemaPullStatus.PULLED;
        } catch (SchemaException ex) {
            logger.warn("Could not find schema version [{}] provided in header for topic [{}]." + " Pulling schema online...", version, topic, ex);
            pullVersionsOnline(topic);
            return SchemaPullStatus.NOT_PULLED;
        }
    }

    private SchemaPullStatus pullSchemaIfNeeded(Topic topic, SchemaId id) {
        if (!rateLimiter.tryAcquireOnlineCheckPermit()) {
            return SchemaPullStatus.NOT_PULLED;
        }
        try {
            schemaRepository.getAvroSchema(topic, id);
            return SchemaPullStatus.PULLED;
        } catch (SchemaException ex) {
            logger.warn("Could not find schema id [{}] provided in header for topic [{}]." + " Pulling schema online...", id, topic, ex);
            return SchemaPullStatus.NOT_PULLED;
        }
    }

    private void pullVersionsOnline(Topic topic) {
        schemaRepository.refreshVersions(topic);
    }

    enum SchemaPullStatus {
        PULLED, NOT_PULLED
    }
}
