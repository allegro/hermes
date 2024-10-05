package pl.allegro.tech.hermes.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageHeaderSchemaIdContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageHeaderSchemaVersionContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageSchemaIdAwareContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageSchemaVersionTruncationContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.schema.SchemaRepository;

@Configuration
@EnableConfigurationProperties(MessageProperties.class)
public class MessageConfiguration {

  @Bean
  CompositeMessageContentWrapper messageContentWrapper(
      MessageProperties messageProperties,
      Clock clock,
      ObjectMapper objectMapper,
      SchemaRepository schemaRepository,
      MetricsFacade metricsFacade) {
    AvroMessageContentWrapper avroWrapper = new AvroMessageContentWrapper(clock);
    JsonMessageContentWrapper jsonWrapper =
        jsonMessageContentWrapper(messageProperties, objectMapper);

    AvroMessageHeaderSchemaVersionContentWrapper headerSchemaVersionWrapper =
        new AvroMessageHeaderSchemaVersionContentWrapper(
            schemaRepository, avroWrapper, metricsFacade);

    AvroMessageHeaderSchemaIdContentWrapper headerSchemaIdWrapper =
        new AvroMessageHeaderSchemaIdContentWrapper(
            schemaRepository,
            avroWrapper,
            metricsFacade,
            messageProperties.isSchemaIdHeaderEnabled());

    AvroMessageSchemaIdAwareContentWrapper schemaAwareWrapper =
        new AvroMessageSchemaIdAwareContentWrapper(schemaRepository, avroWrapper, metricsFacade);

    AvroMessageSchemaVersionTruncationContentWrapper schemaVersionTruncationContentWrapper =
        new AvroMessageSchemaVersionTruncationContentWrapper(
            schemaRepository,
            avroWrapper,
            metricsFacade,
            messageProperties.isSchemaVersionTruncationEnabled());

    return new CompositeMessageContentWrapper(
        jsonWrapper,
        avroWrapper,
        schemaAwareWrapper,
        headerSchemaVersionWrapper,
        headerSchemaIdWrapper,
        schemaVersionTruncationContentWrapper);
  }

  private JsonMessageContentWrapper jsonMessageContentWrapper(
      MessageProperties messageProperties, ObjectMapper objectMapper) {
    return new JsonMessageContentWrapper(
        messageProperties.getContentRoot(),
        messageProperties.getMetadataContentRoot(),
        objectMapper);
  }
}
