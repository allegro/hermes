package pl.allegro.tech.hermes.common.message.wrapper;

import com.codahale.metrics.Counter;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import javax.inject.Inject;
import java.util.List;

public class AvroMessageAnySchemaVersionContentWrapper {

    private static final Logger logger = LoggerFactory.getLogger(AvroMessageAnySchemaVersionContentWrapper.class);

    private final SchemaRepository schemaRepository;
    private final SchemaOnlineChecksRateLimiter schemaOnlineChecksRateLimiter;
    private final AvroMessageContentWrapper avroMessageContentWrapper;

    private final Counter deserializationErrorsForAnySchemaVersion;
    private final Counter deserializationErrorsForAnyOnlineSchemaVersion;

    @Inject
    public AvroMessageAnySchemaVersionContentWrapper(SchemaRepository schemaRepository,
                                                     SchemaOnlineChecksRateLimiter schemaOnlineChecksRateLimiter,
                                                     AvroMessageContentWrapper avroMessageContentWrapper,
                                                     DeserializationMetrics deserializationMetrics) {
        this.schemaRepository = schemaRepository;
        this.schemaOnlineChecksRateLimiter = schemaOnlineChecksRateLimiter;
        this.avroMessageContentWrapper = avroMessageContentWrapper;

        this.deserializationErrorsForAnySchemaVersion = deserializationMetrics.errorsForAnySchemaVersion();
        this.deserializationErrorsForAnyOnlineSchemaVersion = deserializationMetrics.errorsForAnyOnlineSchemaVersion();
    }

    // try-harding to find proper schema
    public UnwrappedMessageContent unwrap(byte[] data, Topic topic) {
        try {
            return tryDeserializingUsingAnySchemaVersion(data, topic, false);
        } catch (Exception ex) {
            logger.info("Trying to find schema online for message for topic {}", topic.getQualifiedName());
            return tryDeserializingUsingAnySchemaVersion(data, topic, true);
        }
    }

    private UnwrappedMessageContent tryDeserializingUsingAnySchemaVersion(byte[] data, Topic topic, boolean online) {
        if (online) {
            limitSchemaRepositoryOnlineCallsRate(topic);
        }

        List<SchemaVersion> versions = schemaRepository.getVersions(topic, online);
        for (SchemaVersion version : versions) {
            try {
                CompiledSchema<Schema> schema =
                        online ?
                                schemaRepository.getKnownAvroSchemaVersion(topic, version) :
                                schemaRepository.getAvroSchema(topic, version);
                return avroMessageContentWrapper.unwrapContent(data, schema);
            } catch (Exception ex) {
                logger.error("Failed to match schema for message for topic {}, schema version {}, fallback to previous.",
                        topic.getQualifiedName(), version.value(), ex);
            }
        }

        logger.error("Could not match schema {} for message of topic {} {}",
                online ? "online" : "from cache", topic.getQualifiedName(), SchemaVersion.toString(versions));
        deserializationErrorsCounterForAnySchemaVersion(online).inc();

        throw new SchemaMissingException(topic);
    }

    private void limitSchemaRepositoryOnlineCallsRate(Topic topic) {
        if (!schemaOnlineChecksRateLimiter.tryAcquireOnlineCheckPermit()) {
            logger.error("Could not match schema online for message of topic {} " +
                    "due to too many schema repository requests", topic.getQualifiedName());
            throw new SchemaMissingException(topic);
        }
    }

    private Counter deserializationErrorsCounterForAnySchemaVersion(boolean online) {
        return online ? deserializationErrorsForAnyOnlineSchemaVersion : deserializationErrorsForAnySchemaVersion;
    }
}
