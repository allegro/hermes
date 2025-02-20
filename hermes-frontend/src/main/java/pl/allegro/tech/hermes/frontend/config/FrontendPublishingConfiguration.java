package pl.allegro.tech.hermes.frontend.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import jakarta.inject.Named;
import java.time.Clock;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.metric.ThroughputRegistry;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.handlers.HandlersChainFactory;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiterFactory;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.DefaultTrackingHeaderExtractor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageEndProcessor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.TrackingHeadersExtractor;
import pl.allegro.tech.hermes.frontend.publishing.message.AvroEnforcer;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageContentTypeEnforcer;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageFactory;
import pl.allegro.tech.hermes.frontend.publishing.metadata.DefaultHeadersPropagator;
import pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersPropagator;
import pl.allegro.tech.hermes.frontend.publishing.preview.DefaultMessagePreviewPersister;
import pl.allegro.tech.hermes.frontend.publishing.preview.MessagePreviewFactory;
import pl.allegro.tech.hermes.frontend.publishing.preview.MessagePreviewLog;
import pl.allegro.tech.hermes.frontend.server.auth.AuthenticationConfiguration;
import pl.allegro.tech.hermes.frontend.validator.MessageValidators;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

@Configuration
@EnableConfigurationProperties({
  ThroughputProperties.class,
  MessagePreviewProperties.class,
  HandlersChainProperties.class,
  SchemaProperties.class
})
public class FrontendPublishingConfiguration {

  @Bean
  public HttpHandler httpHandler(
      TopicsCache topicsCache,
      MessageErrorProcessor messageErrorProcessor,
      MessageEndProcessor messageEndProcessor,
      MessageFactory messageFactory,
      @Named("kafkaBrokerMessageProducer") BrokerMessageProducer brokerMessageProducer,
      MessagePreviewLog messagePreviewLog,
      ThroughputLimiter throughputLimiter,
      Optional<AuthenticationConfiguration> authConfig,
      MessagePreviewProperties messagePreviewProperties,
      HandlersChainProperties handlersChainProperties) {
    return new HandlersChainFactory(
            topicsCache,
            messageErrorProcessor,
            messageEndProcessor,
            messageFactory,
            brokerMessageProducer,
            messagePreviewLog,
            throughputLimiter,
            authConfig,
            messagePreviewProperties.isEnabled(),
            handlersChainProperties)
        .provide();
  }

  @Bean
  public ThroughputRegistry throughputRegistry(MetricsFacade metricsFacade) {
    return new ThroughputRegistry(metricsFacade, new MetricRegistry());
  }

  @Bean
  public ThroughputLimiter throughputLimiter(
      ThroughputProperties throughputProperties, ThroughputRegistry throughputRegistry) {
    return new ThroughputLimiterFactory(throughputProperties, throughputRegistry).provide();
  }

  @Bean
  public MessageEndProcessor messageEndProcessor(
      Trackers trackers,
      BrokerListeners brokerListeners,
      TrackingHeadersExtractor trackingHeadersExtractor) {
    return new MessageEndProcessor(trackers, brokerListeners, trackingHeadersExtractor);
  }

  @Bean
  public TrackingHeadersExtractor extraHeadersExtractor() {
    return new DefaultTrackingHeaderExtractor();
  }

  @Bean
  public MessageErrorProcessor messageErrorProcessor(
      ObjectMapper objectMapper,
      Trackers trackers,
      TrackingHeadersExtractor trackingHeadersExtractor) {
    return new MessageErrorProcessor(objectMapper, trackers, trackingHeadersExtractor);
  }

  @Bean
  public AvroEnforcer messageContentTypeEnforcer() {
    return new MessageContentTypeEnforcer();
  }

  @Bean
  public MessageFactory messageFactory(
      MessageValidators validators,
      AvroEnforcer enforcer,
      SchemaRepository schemaRepository,
      HeadersPropagator headersPropagator,
      CompositeMessageContentWrapper compositeMessageContentWrapper,
      Clock clock,
      SchemaProperties schemaProperties) {
    return new MessageFactory(
        validators,
        enforcer,
        schemaRepository,
        headersPropagator,
        compositeMessageContentWrapper,
        clock,
        schemaProperties.isIdHeaderEnabled());
  }

  @Bean
  public HeadersPropagator defaultHeadersPropagator(HTTPHeadersProperties httpHeadersProperties) {
    return new DefaultHeadersPropagator(httpHeadersProperties);
  }

  @Bean
  public MessagePreviewFactory messagePreviewFactory(
      MessagePreviewProperties messagePreviewProperties) {
    return new MessagePreviewFactory(messagePreviewProperties.getMaxSizeKb());
  }

  @Bean
  public MessagePreviewLog messagePreviewLog(
      MessagePreviewFactory messagePreviewFactory,
      MessagePreviewProperties messagePreviewProperties) {
    return new MessagePreviewLog(messagePreviewFactory, messagePreviewProperties.getSize());
  }

  @Bean
  public DefaultMessagePreviewPersister messagePreviewPersister(
      MessagePreviewLog messagePreviewLog,
      MessagePreviewRepository repository,
      MessagePreviewProperties messagePreviewProperties) {
    return new DefaultMessagePreviewPersister(
        messagePreviewLog,
        repository,
        messagePreviewProperties.getLogPersistPeriod(),
        messagePreviewProperties.isEnabled());
  }
}
