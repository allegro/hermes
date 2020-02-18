package pl.allegro.tech.hermes.common.message.wrapper;

import com.codahale.metrics.Counter;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;

import static pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentUnwrapperResult.AvroMessageContentUnwrapperResultStatus.SUCCESS;

public class AvroMessageAnySchemaVersionContentWrapper implements AvroMessageContentUnwrapper {

    private static final Logger logger = LoggerFactory.getLogger(AvroMessageAnySchemaVersionContentWrapper.class);

    private final SchemaRepository schemaRepository;
    private final SchemaOnlineChecksRateLimiter schemaOnlineChecksRateLimiter;
    private final AvroMessageContentWrapper avroMessageContentWrapper;

    private final Counter deserializationErrorsForAnySchemaVersion;
    private final Counter deserializationErrorsForAnyOnlineSchemaVersion;
    private final Counter deserializationUsingAnySchemaVersion;

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
        this.deserializationUsingAnySchemaVersion = deserializationMetrics.usingAnySchemaVersion();
    }

    @Override
    public AvroMessageContentUnwrapperResult unwrap(byte[] data, Topic topic, @Nullable Integer headerVersion) {
        return unwrap(data, topic);
    }

    @Override
    public boolean isApplicable(byte[] data, Topic topic, Integer schemaVersion) {
        return true;
    }

    // try-harding to find proper schema
    private AvroMessageContentUnwrapperResult unwrap(byte[] data, Topic topic) {
        deserializationUsingAnySchemaVersion.inc();
        AvroMessageContentUnwrapperResult result = tryDeserializingUsingAnySchemaVersion(data, topic, false);

        if (result.getStatus() == SUCCESS) {
            return result;
        } else {
            logger.info("Trying to find schema online for message for topic {}", topic.getQualifiedName());
            return tryDeserializingUsingAnySchemaVersion(data, topic, true);
        }
    }

    private AvroMessageContentUnwrapperResult tryDeserializingUsingAnySchemaVersion(byte[] data, Topic topic, boolean online) {
        if (online) {
            if (!schemaOnlineChecksRateLimiter.tryAcquireOnlineCheckPermit()) {
                logger.error("Could not match schema online for message of topic {} " +
                        "due to too many schema repository requests", topic.getQualifiedName());
                return AvroMessageContentUnwrapperResult.failure();
            }
        }

        List<SchemaVersion> versions = schemaRepository.getVersions(topic, online);
        for (SchemaVersion version : versions) {
            try {
                CompiledSchema<Schema> schema =
                        online ?
                                schemaRepository.getKnownAvroSchemaVersion(topic, version) :
                                schemaRepository.getAvroSchema(topic, version);
                return AvroMessageContentUnwrapperResult.success(avroMessageContentWrapper.unwrapContent(data, schema));
            } catch (Exception ex) {
                logger.error("Failed to match schema for message for topic {}, schema version {}, fallback to previous version.",
                        topic.getQualifiedName(), version.value(), ex);
            }
        }

        deserializationErrorsCounterForAnySchemaVersion(online).inc();
        logger.error("Could not match schema {} for message of topic {} {}",
                online ? "online" : "from cache", topic.getQualifiedName(), SchemaVersion.toString(versions));

        return AvroMessageContentUnwrapperResult.failure();
    }

    private Counter deserializationErrorsCounterForAnySchemaVersion(boolean online) {
        return online ? deserializationErrorsForAnyOnlineSchemaVersion : deserializationErrorsForAnySchemaVersion;
    }
}
