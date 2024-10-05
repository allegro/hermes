package pl.allegro.tech.hermes.benchmark.environment;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.benchmark.environment.HermesServerEnvironment.loadMessageResource;
import static pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter.QuotaInsight.quotaConfirmed;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.undertow.server.HttpHandler;
import java.io.IOException;
import java.time.Clock;
import java.util.Collections;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.config.HTTPHeadersProperties;
import pl.allegro.tech.hermes.frontend.config.HandlersChainProperties;
import pl.allegro.tech.hermes.frontend.config.HermesServerProperties;
import pl.allegro.tech.hermes.frontend.config.SchemaProperties;
import pl.allegro.tech.hermes.frontend.config.SslProperties;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.handlers.HandlersChainFactory;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.DefaultTrackingHeaderExtractor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageEndProcessor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.TrackingHeadersExtractor;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageContentTypeEnforcer;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageFactory;
import pl.allegro.tech.hermes.frontend.publishing.metadata.DefaultHeadersPropagator;
import pl.allegro.tech.hermes.frontend.readiness.HealthCheckService;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.frontend.validator.MessageValidators;
import pl.allegro.tech.hermes.schema.DirectCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.DirectSchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaCompilersFactory;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

class HermesServerFactory {

  private static final Topic topic =
      topic(HermesServerEnvironment.BENCHMARK_TOPIC).withContentType(AVRO).build();

  static HermesServer provideHermesServer() throws IOException {
    ThroughputLimiter throughputLimiter = (exampleTopic, throughput) -> quotaConfirmed();
    MetricsFacade metricsFacade = new MetricsFacade(new SimpleMeterRegistry());
    TopicsCache topicsCache = new InMemoryTopicsCache(metricsFacade, topic);
    BrokerMessageProducer brokerMessageProducer = new InMemoryBrokerMessageProducer();
    RawSchemaClient rawSchemaClient =
        new InMemorySchemaClient(topic.getName(), loadMessageResource("schema"), 1, 1);
    Trackers trackers = new Trackers(Collections.emptyList());
    AvroMessageContentWrapper avroMessageContentWrapper =
        new AvroMessageContentWrapper(Clock.systemDefaultZone());
    HttpHandler httpHandler =
        provideHttpHandler(
            throughputLimiter,
            topicsCache,
            brokerMessageProducer,
            rawSchemaClient,
            trackers,
            avroMessageContentWrapper);
    SslProperties sslProperties = new SslProperties();
    HermesServerProperties hermesServerProperties = new HermesServerProperties();
    hermesServerProperties.setGracefulShutdownEnabled(false);

    return new HermesServer(
        sslProperties,
        hermesServerProperties,
        metricsFacade,
        httpHandler,
        new HealthCheckService(),
        new DisabledReadinessChecker(false),
        new NoOpMessagePreviewPersister(),
        throughputLimiter,
        null,
        null);
  }

  private static HttpHandler provideHttpHandler(
      ThroughputLimiter throughputLimiter,
      TopicsCache topicsCache,
      BrokerMessageProducer brokerMessageProducer,
      RawSchemaClient rawSchemaClient,
      Trackers trackers,
      AvroMessageContentWrapper avroMessageContentWrapper) {
    HTTPHeadersProperties httpHeadersProperties = new HTTPHeadersProperties();
    HandlersChainProperties handlersChainProperties = new HandlersChainProperties();
    TrackingHeadersExtractor trackingHeadersExtractor = new DefaultTrackingHeaderExtractor();
    SchemaProperties schemaProperties = new SchemaProperties();

    return new HandlersChainFactory(
            topicsCache,
            new MessageErrorProcessor(new ObjectMapper(), trackers, trackingHeadersExtractor),
            new MessageEndProcessor(trackers, new BrokerListeners(), trackingHeadersExtractor),
            new MessageFactory(
                new MessageValidators(Collections.emptyList()),
                new MessageContentTypeEnforcer(),
                new SchemaRepository(
                    new DirectSchemaVersionsRepository(rawSchemaClient),
                    new DirectCompiledSchemaRepository<>(
                        rawSchemaClient, SchemaCompilersFactory.avroSchemaCompiler())),
                new DefaultHeadersPropagator(httpHeadersProperties),
                new BenchmarkMessageContentWrapper(avroMessageContentWrapper),
                Clock.systemDefaultZone(),
                schemaProperties.isIdHeaderEnabled()),
            brokerMessageProducer,
            null,
            throughputLimiter,
            null,
            false,
            handlersChainProperties)
        .provide();
  }
}
