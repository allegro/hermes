package pl.allegro.tech.hermes.benchmark.environment;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopics;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.IMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.SchemaAwareSerDe;
import pl.allegro.tech.hermes.common.message.wrapper.UnwrappedMessageContent;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.handlers.HandlersChainFactory;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageEndProcessor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageContentTypeEnforcer;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageFactory;
import pl.allegro.tech.hermes.frontend.publishing.metadata.DefaultHeadersPropagator;
import pl.allegro.tech.hermes.frontend.publishing.preview.IMessagePreviewPersister;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.frontend.server.IReadinessChecker;
import pl.allegro.tech.hermes.frontend.validator.MessageValidators;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.schema.CompiledSchema;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;
import static pl.allegro.tech.hermes.benchmark.environment.FrontendEnvironment.loadMessageResource;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_TOPIC_METADATA_REFRESH_JOB_ENABLED;
import static pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter.QuotaInsight.quotaConfirmed;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class BenchmarkProviders {

    HermesServer provideHermesServer() throws IOException {
        ThroughputLimiter throughputLimiter = (topic, throughput) -> quotaConfirmed();
        HermesMetrics hermesMetrics = new HermesMetrics(new MetricRegistry(), new PathsCompiler(""));
        TopicsCache topicsCache = new TopicsCache() {
            @Override
            public Optional<CachedTopic> getTopic(String qualifiedTopicName) {
                Topic topic = topic("bench.topic").withContentType(AVRO).build();
                return Optional.of(
                        new CachedTopic(
                                topic,
                                hermesMetrics,
                                new KafkaTopics(new KafkaTopic(KafkaTopicName.valueOf(""), AVRO))
                        )
                );
            }

            @Override
            public List<CachedTopic> getTopics() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void start() {
                throw new UnsupportedOperationException();
            }
        };
        BrokerMessageProducer brokerMessageProducer = new BrokerMessageProducer() {
            @Override
            public void send(Message message, CachedTopic topic, PublishingCallback callback) {
                callback.onPublished(message, topic.getTopic());
            }

            @Override
            public boolean isTopicAvailable(CachedTopic topic) {
                return true;
            }
        };
        RawSchemaClient rawSchemaClient = new InMemorySchemaClient(fromQualifiedName("bench.topic"), loadMessageResource("schema"), 1, 1);
        ConfigFactory configFactory = new MutableConfigFactory()
                .overrideProperty(FRONTEND_TOPIC_METADATA_REFRESH_JOB_ENABLED, false);
        Trackers trackers = new Trackers(Collections.emptyList());
        AvroMessageContentWrapper avroMessageContentWrapper = new AvroMessageContentWrapper(Clock.systemDefaultZone());
        HttpHandler httpHandler = new HandlersChainFactory(
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
                        new IMessageContentWrapper() {
                            @Override
                            public UnwrappedMessageContent unwrapAvro(byte[] data, Topic topic, Integer schemaId, Integer schemaVersion) {
                                throw new UnsupportedOperationException();
                            }

                            @Override
                            public UnwrappedMessageContent unwrapJson(byte[] data) {
                                throw new UnsupportedOperationException();
                            }

                            @Override
                            public byte[] wrapAvro(byte[] data, String id, long timestamp, Topic topic, CompiledSchema<Schema> schema, Map<String, String> externalMetadata) {
                                byte[] wrapped = avroMessageContentWrapper.wrapContent(data, id, timestamp, schema.getSchema(), externalMetadata);
                                return topic.isSchemaIdAwareSerializationEnabled() ? SchemaAwareSerDe.serialize(schema.getId(), wrapped) : wrapped;
                            }

                            @Override
                            public byte[] wrapJson(byte[] data, String id, long timestamp, Map<String, String> externalMetadata) {
                                throw new UnsupportedOperationException();
                            }
                        },
                        Clock.systemDefaultZone(),
                        configFactory
                ),
                brokerMessageProducer,
                null,
                throughputLimiter,
                null
        ).provide();

        return new HermesServer(
                configFactory,
                hermesMetrics,
                httpHandler,
                new IReadinessChecker() {
                    @Override
                    public boolean isReady() {
                        return false;
                    }

                    @Override
                    public void start() {
                    }

                    @Override
                    public void stop() {
                    }
                },
                new IMessagePreviewPersister() {
                    @Override
                    public void start() {
                    }

                    @Override
                    public void shutdown() {
                    }
                },
                throughputLimiter,
                null,
                null
        );
    }
}
