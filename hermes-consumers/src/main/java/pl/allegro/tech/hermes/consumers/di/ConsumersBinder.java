package pl.allegro.tech.hermes.consumers.di;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.di.factories.UndeliveredMessageLogFactory;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerMessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.batch.ByteBufferMessageBatchFactoryProvider;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchFactory;
import pl.allegro.tech.hermes.consumers.consumer.converter.AvroToJsonMessageConverter;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.converter.NoOperationMessageConverter;
import pl.allegro.tech.hermes.consumers.consumer.converter.schema.AvroSchemaRepositoryMetadataAware;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.MessageBodyInterpolator;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.UriInterpolator;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsStorage;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.BlockingChannelFactory;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.BrokerOffsetsRepository;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.KafkaOffsetsStorage;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.zookeeper.ZookeeperOffsetsStorage;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimitSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculator;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaMessageReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.MessageCommitterFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.OffsetStoragesFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.HttpMessageBatchSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.DefaultHttpMetadataAppender;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.JettyHttpMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.jms.JmsHornetQMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.jms.JmsMetadataAppender;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.InterpolatingEndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeoutFactory;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;
import pl.allegro.tech.hermes.consumers.health.HealthCheckServer;
import pl.allegro.tech.hermes.consumers.message.undelivered.UndeliveredMessageLogPersister;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.subscription.cache.zookeeper.ZookeeperSubscriptionsCacheFactory;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerFactory;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersExecutorService;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SupervisorController;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SupervisorControllerFactory;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkTracker;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkTrackerFactory;

import javax.inject.Singleton;
import javax.jms.Message;
import java.util.List;

public class ConsumersBinder extends AbstractBinder {

    @Override
    protected void configure() {
        bindSingleton(HealthCheckServer.class);

        bind(KafkaMessageReceiverFactory.class).in(Singleton.class).to(ReceiverFactory.class);
        bindSingleton(BrokerOffsetsRepository.class);
        bind(ZookeeperOffsetsStorage.class).in(Singleton.class).to(OffsetsStorage.class).named("zookeeperOffsetsStorage");
        bind(KafkaOffsetsStorage.class).in(Singleton.class).to(OffsetsStorage.class).named("kafkaOffsetsStorage");
        bindFactory(MessageCommitterFactory.class).in(Singleton.class).to(new TypeLiteral<List<MessageCommitter>>() {
        });
        bind(MessageBodyInterpolator.class).in(Singleton.class).to(UriInterpolator.class);
        bind(InterpolatingEndpointAddressResolver.class).to(EndpointAddressResolver.class).in(Singleton.class);
        bind(JmsHornetQMessageSenderProvider.class).to(ProtocolMessageSenderProvider.class)
                .in(Singleton.class).named("defaultJmsMessageSenderProvider");
        bind(JettyHttpMessageSenderProvider.class).to(ProtocolMessageSenderProvider.class)
                .in(Singleton.class).named("defaultHttpMessageSenderProvider");

        bind("consumer").named("moduleName").to(String.class);

        bindSingleton(ConsumersSupervisor.class);
        bindSingleton(MessageSenderFactory.class);
        bindSingleton(ConsumerFactory.class);
        bindSingleton(ConsumerRateLimitSupervisor.class);
        bindSingleton(OutputRateCalculator.class);
        bindSingleton(ConsumersExecutorService.class);
        bindSingleton(ZookeeperAdminCache.class);
        bindSingleton(InstrumentedExecutorServiceFactory.class);
        bindSingleton(ConsumerMessageSenderFactory.class);
        bindSingleton(NoOperationMessageConverter.class);
        bindSingleton(AvroToJsonMessageConverter.class);
        bindSingleton(MessageConverterResolver.class);
        bindSingleton(AvroSchemaRepositoryMetadataAware.class);
        bind(JmsMetadataAppender.class).in(Singleton.class).to(new TypeLiteral<MetadataAppender<Message>>() {});
        bind(DefaultHttpMetadataAppender.class).in(Singleton.class).to(new TypeLiteral<MetadataAppender<Request>>() {});

        bindSingleton(BlockingChannelFactory.class);
        bindFactory(OffsetStoragesFactory.class).in(Singleton.class).to(new TypeLiteral<List<OffsetsStorage>>() {});
        bindFactory(FutureAsyncTimeoutFactory.class).in(Singleton.class).to(new TypeLiteral<FutureAsyncTimeout<MessageSendingResult>>(){});
        bindFactory(HttpClientFactory.class).in(Singleton.class).to(HttpClient.class);
        bindFactory(ZookeeperSubscriptionsCacheFactory.class).to(SubscriptionsCache.class).in(Singleton.class);

        bindFactory(UndeliveredMessageLogFactory.class).in(Singleton.class).to(UndeliveredMessageLog.class);
        bindFactory(WorkTrackerFactory.class).in(Singleton.class).to(WorkTracker.class);
        bindFactory(SupervisorControllerFactory.class).in(Singleton.class).to(SupervisorController.class);

        bindSingleton(UndeliveredMessageLogPersister.class);
        bindFactory(ByteBufferMessageBatchFactoryProvider.class).in(Singleton.class).to(MessageBatchFactory.class);
        bind(HttpMessageBatchSenderFactory.class).to(MessageBatchSenderFactory.class).in(Singleton.class);
    }

    private <T> void bindSingleton(Class<T> clazz) {
        bind(clazz).in(Singleton.class).to(clazz);
    }
}
