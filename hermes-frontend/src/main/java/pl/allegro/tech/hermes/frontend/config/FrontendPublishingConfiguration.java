package pl.allegro.tech.hermes.frontend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.handlers.HandlersChainFactory;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiterFactory;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.DefaultTrackingHeaderExtractor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.TrackingHeadersExtractor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageEndProcessor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.AvroEnforcer;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageContentTypeEnforcer;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageFactory;
import pl.allegro.tech.hermes.frontend.publishing.metadata.DefaultHeadersPropagator;
import pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersPropagator;
import pl.allegro.tech.hermes.frontend.publishing.preview.MessagePreviewFactory;
import pl.allegro.tech.hermes.frontend.publishing.preview.MessagePreviewLog;
import pl.allegro.tech.hermes.frontend.publishing.preview.DefaultMessagePreviewPersister;
import pl.allegro.tech.hermes.frontend.server.auth.AuthenticationConfiguration;
import pl.allegro.tech.hermes.frontend.validator.MessageValidators;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

import java.time.Clock;
import java.util.Optional;

@Configuration
public class FrontendPublishingConfiguration {

    @Bean
    public HttpHandler httpHandler(TopicsCache topicsCache, MessageErrorProcessor messageErrorProcessor,
                                   MessageEndProcessor messageEndProcessor, ConfigFactory configFactory, MessageFactory messageFactory,
                                   BrokerMessageProducer brokerMessageProducer, MessagePreviewLog messagePreviewLog,
                                   ThroughputLimiter throughputLimiter, Optional<AuthenticationConfiguration> authConfig) {
        return new HandlersChainFactory(topicsCache, messageErrorProcessor, messageEndProcessor, configFactory, messageFactory,
                brokerMessageProducer, messagePreviewLog, throughputLimiter, authConfig).provide();
    }

    @Bean
    public ThroughputLimiter throughputLimiter(ConfigFactory configs, HermesMetrics hermesMetrics) {
        return new ThroughputLimiterFactory(configs, hermesMetrics).provide();
    }

    @Bean
    public MessageEndProcessor messageEndProcessor(Trackers trackers, BrokerListeners brokerListeners,
                                                   TrackingHeadersExtractor trackingHeadersExtractor) {
        return new MessageEndProcessor(trackers, brokerListeners, trackingHeadersExtractor);
    }

    @Bean
    public TrackingHeadersExtractor extraHeadersExtractor() {
        return new DefaultTrackingHeaderExtractor();
    }

    @Bean
    public MessageErrorProcessor messageErrorProcessor(ObjectMapper objectMapper, Trackers trackers,
                                                       TrackingHeadersExtractor trackingHeadersExtractor) {
        return new MessageErrorProcessor(objectMapper, trackers, trackingHeadersExtractor);
    }

    @Bean
    public AvroEnforcer messageContentTypeEnforcer() {
        return new MessageContentTypeEnforcer();
    }

    @Bean
    public MessageFactory messageFactory(MessageValidators validators,
                                         AvroEnforcer enforcer,
                                         SchemaRepository schemaRepository,
                                         HeadersPropagator headersPropagator,
                                         CompositeMessageContentWrapper compositeMessageContentWrapper,
                                         Clock clock,
                                         ConfigFactory configFactory) {
        return new MessageFactory(validators, enforcer, schemaRepository, headersPropagator, compositeMessageContentWrapper,
                clock, configFactory);
    }

    @Bean
    public HeadersPropagator defaultHeadersPropagator(ConfigFactory config) {
        return new DefaultHeadersPropagator(config);
    }

    @Bean
    public MessagePreviewFactory messagePreviewFactory(ConfigFactory configFactory) {
        return new MessagePreviewFactory(configFactory);
    }

    @Bean
    public MessagePreviewLog messagePreviewLog(MessagePreviewFactory messagePreviewFactory,
                                               ConfigFactory configFactory) {
        return new MessagePreviewLog(messagePreviewFactory, configFactory);
    }

    @Bean
    public DefaultMessagePreviewPersister messagePreviewPersister(MessagePreviewLog messagePreviewLog,
                                                                  MessagePreviewRepository repository,
                                                                  ConfigFactory configFactory) {
        return new DefaultMessagePreviewPersister(messagePreviewLog, repository, configFactory);
    }
}
