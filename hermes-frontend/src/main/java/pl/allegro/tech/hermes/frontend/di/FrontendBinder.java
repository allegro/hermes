package pl.allegro.tech.hermes.frontend.di;

import io.undertow.server.HttpHandler;
import org.glassfish.hk2.api.TypeLiteral;
import pl.allegro.tech.hermes.common.di.AbstractBinder;
import pl.allegro.tech.hermes.common.hook.HooksHandler;
import pl.allegro.tech.hermes.frontend.buffer.BackupMessagesLoader;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCacheFactory;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaBrokerMessageProducerFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaMessageProducerFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.Producers;
import pl.allegro.tech.hermes.frontend.publishing.handlers.HandlersChainFactory;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageEndProcessor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageContentTypeEnforcer;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageFactory;
import pl.allegro.tech.hermes.frontend.publishing.metadata.DefaultHeadersPropagator;
import pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersPropagator;
import pl.allegro.tech.hermes.frontend.publishing.preview.MessagePreviewLog;
import pl.allegro.tech.hermes.frontend.publishing.preview.MessagePreviewPersister;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.frontend.server.TopicMetadataLoadingStartupHook;
import pl.allegro.tech.hermes.frontend.server.TopicSchemaLoadingStartupHook;
import pl.allegro.tech.hermes.frontend.services.HealthCheckService;
import pl.allegro.tech.hermes.frontend.validator.AvroTopicMessageValidator;
import pl.allegro.tech.hermes.frontend.validator.MessageValidators;
import pl.allegro.tech.hermes.frontend.validator.TopicMessageValidator;
import pl.allegro.tech.hermes.frontend.validator.TopicMessageValidatorListFactory;
import pl.allegro.tech.hermes.tracker.frontend.NoOperationPublishingTracker;
import pl.allegro.tech.hermes.tracker.frontend.PublishingMessageTracker;

import javax.inject.Singleton;
import java.util.List;

public class FrontendBinder extends AbstractBinder {

    private final HooksHandler hooksHandler;

    public FrontendBinder(HooksHandler hooksHandler) {
        this.hooksHandler = hooksHandler;
    }

    @Override
    protected void configure() {
        bindSingleton(HermesServer.class);
        bindSingleton(MessageErrorProcessor.class);
        bindSingleton(MessageEndProcessor.class);
        bindSingleton(MessageValidators.class);

        bind(hooksHandler).to(HooksHandler.class);
        bindSingleton(TopicMetadataLoadingStartupHook.class);
        bindSingleton(TopicSchemaLoadingStartupHook.class);

        bind("producer").named("moduleName").to(String.class);

        bindSingleton(HealthCheckService.class);
        bind(DefaultHeadersPropagator.class).to(HeadersPropagator.class).in(Singleton.class);

        bindFactory(HandlersChainFactory.class).to(HttpHandler.class).in(Singleton.class);
        bindFactory(KafkaMessageProducerFactory.class).to(Producers.class).in(Singleton.class);
        bindFactory(KafkaBrokerMessageProducerFactory.class).to(BrokerMessageProducer.class).in(Singleton.class);
        bindSingleton(PublishingMessageTracker.class);
        bindSingleton(NoOperationPublishingTracker.class);
        bindFactory(TopicsCacheFactory.class).to(TopicsCache.class).in(Singleton.class);
        bindSingleton(MessageContentTypeEnforcer.class);
        bindSingleton(AvroTopicMessageValidator.class);
        bindFactory(TopicMessageValidatorListFactory.class).in(Singleton.class).to(new TypeLiteral<List<TopicMessageValidator>>() {
        });
        bindSingleton(MessageFactory.class);
        bindSingleton(BackupMessagesLoader.class);
        bindSingleton(PersistentBufferExtension.class);
        bindSingleton(MessagePreviewPersister.class);
        bindSingleton(MessagePreviewLog.class);
        bindSingletonFactory(BlacklistZookeeperNotifyingCacheFactory.class);
    }

}
