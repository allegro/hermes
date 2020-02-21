package pl.allegro.tech.hermes.management.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageAnySchemaVersionContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageHeaderSchemaVersionContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageSchemaVersionAwareContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.DeserializationMetrics;
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
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
    MessageContentWrapper messageContentWrapper() {
        DeserializationMetrics metrics = new DeserializationMetrics(metricRegistry);
        AvroMessageContentWrapper avroWrapper = new AvroMessageContentWrapper(clock);
        JsonMessageContentWrapper jsonWrapper = jsonMessageContentWrapper();

        AvroMessageAnySchemaVersionContentWrapper anySchemaWrapper =
                new AvroMessageAnySchemaVersionContentWrapper(schemaRepository, () -> true, avroWrapper, metrics);

        AvroMessageHeaderSchemaVersionContentWrapper headerSchemaWrapper =
                new AvroMessageHeaderSchemaVersionContentWrapper(schemaRepository, avroWrapper, metrics);

        AvroMessageSchemaVersionAwareContentWrapper schemaAwareWrapper =
                new AvroMessageSchemaVersionAwareContentWrapper(schemaRepository, avroWrapper, metrics);

        return new MessageContentWrapper(
                jsonWrapper,
                avroWrapper,
                schemaAwareWrapper,
                headerSchemaWrapper,
                anySchemaWrapper);
    }

    private JsonMessageContentWrapper jsonMessageContentWrapper() {
        return new JsonMessageContentWrapper(messageProperties.getContentRoot(), messageProperties.getMetadataContentRoot(), objectMapper);
    }

}
