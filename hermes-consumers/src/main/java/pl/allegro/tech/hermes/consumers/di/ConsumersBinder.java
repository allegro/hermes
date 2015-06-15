package pl.allegro.tech.hermes.consumers.di;

import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.di.factories.UndeliveredMessageLogFactory;
import pl.allegro.tech.hermes.common.json.MessageContentWrapper;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerMessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeoutFactory;
import pl.allegro.tech.hermes.consumers.message.undelivered.UndeliveredMessageLogPersister;
import pl.allegro.tech.hermes.consumers.consumer.health.HealthCheckServer;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.MessageBodyInterpolator;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.UriInterpolator;
import pl.allegro.tech.hermes.consumers.consumer.offset.AsyncOffsetMonitor;
import pl.allegro.tech.hermes.consumers.consumer.offset.AsyncOffsetMonitorFactory;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.KafkaLatestOffsetReader;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimitSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculator;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageSplitter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaMessageCommitter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaMessageReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.JettyHttpMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.jms.JmsHornetQMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.InterpolatingEndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.subscription.cache.zookeeper.ZookeeperSubscriptionsCacheFactory;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerFactory;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersExecutorService;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.utils.SubscriptionSuspender;

import javax.inject.Singleton;

public class ConsumersBinder extends AbstractBinder {

    @Override
    protected void configure() {
        bindSingleton(HealthCheckServer.class);

        bind(KafkaMessageReceiverFactory.class).in(Singleton.class).to(ReceiverFactory.class);
        bind(KafkaMessageCommitter.class).in(Singleton.class).to(MessageCommitter.class);
        bind(MessageBodyInterpolator.class).in(Singleton.class).to(UriInterpolator.class);
        bind(InterpolatingEndpointAddressResolver.class).to(EndpointAddressResolver.class).in(Singleton.class);
        bind(JmsHornetQMessageSenderProvider.class).to(ProtocolMessageSenderProvider.class)
                .in(Singleton.class).named("defaultJmsMessageSenderProvider");
        bind(JettyHttpMessageSenderProvider.class).to(ProtocolMessageSenderProvider.class)
                .in(Singleton.class).named("defaultHttpMessageSenderProvider");

        bindSingleton(ConsumersSupervisor.class);
        bindSingleton(MessageContentWrapper.class);
        bindSingleton(MessageSenderFactory.class);
        bindSingleton(MessageSplitter.class);
        bindSingleton(ConsumerFactory.class);
        bindSingleton(ConsumerRateLimitSupervisor.class);
        bindSingleton(OutputRateCalculator.class);
        bindSingleton(SubscriptionSuspender.class);
        bindSingleton(KafkaLatestOffsetReader.class);
        bindSingleton(ConsumersExecutorService.class);
        bindSingleton(ZookeeperAdminCache.class);
        bindSingleton(InstrumentedExecutorServiceFactory.class);
        bindSingleton(ConsumerMessageSenderFactory.class);

        bindFactory(FutureAsyncTimeoutFactory.class).in(Singleton.class).to(new TypeLiteral<FutureAsyncTimeout<MessageSendingResult>>(){});
        bindFactory(HttpClientFactory.class).in(Singleton.class).to(HttpClient.class);
        bindFactory(AsyncOffsetMonitorFactory.class).in(Singleton.class).to(AsyncOffsetMonitor.class);
        bindFactory(ZookeeperSubscriptionsCacheFactory.class).to(SubscriptionsCache.class).in(Singleton.class);

        bindFactory(UndeliveredMessageLogFactory.class).in(Singleton.class).to(UndeliveredMessageLog.class);
        bindSingleton(UndeliveredMessageLogPersister.class);
    }

    private <T> void bindSingleton(Class<T> clazz) {
        bind(clazz).in(Singleton.class).to(clazz);
    }
}
