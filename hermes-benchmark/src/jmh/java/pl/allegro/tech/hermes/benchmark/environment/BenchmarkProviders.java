package pl.allegro.tech.hermes.benchmark.environment;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.handlers.HandlersChainFactory;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageEndProcessor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageContentTypeEnforcer;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageFactory;
import pl.allegro.tech.hermes.frontend.publishing.metadata.DefaultHeadersPropagator;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.frontend.validator.MessageValidators;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.schema.DirectCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.DirectSchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaCompilersFactory;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

import java.io.IOException;
import java.time.Clock;
import java.util.Collections;

import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;
import static pl.allegro.tech.hermes.benchmark.environment.HermesServerEnvironment.loadMessageResource;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_TOPIC_METADATA_REFRESH_JOB_ENABLED;
import static pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter.QuotaInsight.quotaConfirmed;

public class BenchmarkProviders {

    static HermesServer provideHermesServer() throws IOException {
        ThroughputLimiter throughputLimiter = (topic, throughput) -> quotaConfirmed();
        HermesMetrics hermesMetrics = new HermesMetrics(new MetricRegistry(), new PathsCompiler(""));
        TopicsCache topicsCache = new InMemoryTopicsCache(hermesMetrics);
        BrokerMessageProducer brokerMessageProducer = new InMemoryBrokerMessageProducer();
        RawSchemaClient rawSchemaClient = new InMemorySchemaClient(fromQualifiedName("bench.topic"), loadMessageResource("schema"), 1, 1);
        ConfigFactory configFactory = new MutableConfigFactory()
                .overrideProperty(FRONTEND_TOPIC_METADATA_REFRESH_JOB_ENABLED, false);
        Trackers trackers = new Trackers(Collections.emptyList());
        AvroMessageContentWrapper avroMessageContentWrapper = new AvroMessageContentWrapper(Clock.systemDefaultZone());
        HttpHandler httpHandler = provideHttpHandler(throughputLimiter, topicsCache, brokerMessageProducer, rawSchemaClient, configFactory, trackers, avroMessageContentWrapper);

        return new HermesServer(
                configFactory,
                hermesMetrics,
                httpHandler,
                new EmptyReadinessChecker(),
                new EmptyMessagePreviewPersister(),
                throughputLimiter,
                null,
                null
        );
    }

    private static HttpHandler provideHttpHandler(ThroughputLimiter throughputLimiter, TopicsCache topicsCache, BrokerMessageProducer brokerMessageProducer, RawSchemaClient rawSchemaClient, ConfigFactory configFactory, Trackers trackers, AvroMessageContentWrapper avroMessageContentWrapper) {
        return new HandlersChainFactory(
                topicsCache,
                new MessageErrorProcessor(new ObjectMapper(), trackers),
                new MessageEndProcessor(trackers, new BrokerListeners()),
                configFactory,
                new MessageFactory(
                        new MessageValidators(Collections.emptyList()),
                        new MessageContentTypeEnforcer(),
                        new SchemaRepository(
                                new DirectSchemaVersionsRepository(rawSchemaClient),
                                new DirectCompiledSchemaRepository(rawSchemaClient, SchemaCompilersFactory.avroSchemaCompiler())
                        ),
                        new DefaultHeadersPropagator(configFactory),
                        new BenchmarkMessageContentWrapper(avroMessageContentWrapper),
                        Clock.systemDefaultZone(),
                        configFactory
                ),
                brokerMessageProducer,
                null,
                throughputLimiter,
                null
        ).provide();
    }
}
