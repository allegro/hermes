package pl.allegro.tech.hermes.management.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageHeaderSchemaIdContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageHeaderSchemaVersionContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageSchemaIdAwareContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageSchemaVersionTruncationContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.DeserializationMetrics;
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.schema.SchemaRepository;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(MessageProperties.class)
public class MessageConfiguration {

    @Autowired
    MessageProperties messageProperties;

    @Autowired
    Clock clock;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    SchemaRepository schemaRepository;

    @Autowired
    MetricRegistry metricRegistry;

    @Bean
    CompositeMessageContentWrapper messageContentWrapper() {
        DeserializationMetrics metrics = new DeserializationMetrics(metricRegistry);
        AvroMessageContentWrapper avroWrapper = new AvroMessageContentWrapper(clock);
        JsonMessageContentWrapper jsonWrapper = jsonMessageContentWrapper();

        AvroMessageHeaderSchemaVersionContentWrapper headerSchemaVersionWrapper =
                new AvroMessageHeaderSchemaVersionContentWrapper(schemaRepository, avroWrapper, metrics);

        AvroMessageHeaderSchemaIdContentWrapper headerSchemaIdWrapper =
                new AvroMessageHeaderSchemaIdContentWrapper(schemaRepository, avroWrapper, metrics,
                        messageProperties.isSchemaIdHeaderEnabled());

        AvroMessageSchemaIdAwareContentWrapper schemaAwareWrapper =
                new AvroMessageSchemaIdAwareContentWrapper(schemaRepository, avroWrapper, metrics);

        AvroMessageSchemaVersionTruncationContentWrapper schemaVersionTruncationContentWrapper =
                new AvroMessageSchemaVersionTruncationContentWrapper(schemaRepository, avroWrapper, metrics,
                        messageProperties.isSchemaVersionTruncationEnabled());

        return new CompositeMessageContentWrapper(
                jsonWrapper,
                avroWrapper,
                schemaAwareWrapper,
                headerSchemaVersionWrapper,
                headerSchemaIdWrapper,
                schemaVersionTruncationContentWrapper);
    }

    private JsonMessageContentWrapper jsonMessageContentWrapper() {
        return new JsonMessageContentWrapper(messageProperties.getContentRoot(), messageProperties.getMetadataContentRoot(), objectMapper);
    }

}
