package pl.allegro.tech.hermes.common.di;

import org.apache.avro.Schema;
import org.glassfish.hk2.api.TypeLiteral;
import pl.allegro.tech.hermes.common.clock.ClockFactory;
import pl.allegro.tech.hermes.common.di.factories.CuratorClientFactory;
import pl.allegro.tech.hermes.common.di.factories.ConfigFactoryCreator;
import pl.allegro.tech.hermes.common.di.factories.GraphiteWebTargetFactory;
import pl.allegro.tech.hermes.common.di.factories.GroupRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.HermesCuratorClientFactory;
import pl.allegro.tech.hermes.common.di.factories.MessagePreviewRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.MetricRegistryFactory;
import pl.allegro.tech.hermes.common.di.factories.ModelAwareZookeeperNotifyingCacheFactory;
import pl.allegro.tech.hermes.common.di.factories.OAuthProviderRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.ObjectMapperFactory;
import pl.allegro.tech.hermes.common.di.factories.PathsCompilerFactory;
import pl.allegro.tech.hermes.common.di.factories.SharedCounterFactory;
import pl.allegro.tech.hermes.common.di.factories.SubscriptionOffsetChangeIndicatorFactory;
import pl.allegro.tech.hermes.common.di.factories.SubscriptionRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.TopicRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.WorkloadConstraintsRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.ZookeeperPathsFactory;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapperFactory;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageAnySchemaVersionContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageHeaderSchemaVersionContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.DeserializationMetrics;
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.SchemaOnlineChecksRateLimiter;
import pl.allegro.tech.hermes.common.message.wrapper.SchemaOnlineChecksWaitingRateLimiter;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageSchemaIdAwareContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageHeaderSchemaIdContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageSchemaVersionTruncationContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.metric.counter.zookeeper.ZookeeperCounterStorage;
import pl.allegro.tech.hermes.common.schema.AvroCompiledSchemaRepositoryFactory;
import pl.allegro.tech.hermes.common.schema.SchemaRepositoryClientFactory;
import pl.allegro.tech.hermes.common.schema.SchemaRepositoryInstanceResolverFactory;
import pl.allegro.tech.hermes.common.schema.SchemaRepositoryFactory;
import pl.allegro.tech.hermes.common.schema.RawSchemaClientFactory;
import pl.allegro.tech.hermes.common.schema.SchemaVersionsRepositoryFactory;
import pl.allegro.tech.hermes.common.util.InstanceIdResolver;
import pl.allegro.tech.hermes.common.util.InetAddressInstanceIdResolver;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.notifications.ZookeeperInternalNotificationBus;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;

import javax.inject.Singleton;
import java.time.Clock;

public class CommonBinder extends AbstractBinder {

    @Override
    protected void configure() {
        bind(ZookeeperCounterStorage.class).to(CounterStorage.class).in(Singleton.class);
        bindFactory(ClockFactory.class).in(Singleton.class).to(Clock.class);
        bind(InetAddressInstanceIdResolver.class).in(Singleton.class).to(InstanceIdResolver.class);
        bindSingletonFactory(SchemaRepositoryClientFactory.class);
        bindSingletonFactory(SchemaRepositoryInstanceResolverFactory.class);
        bindSingletonFactory(RawSchemaClientFactory.class);
        bindSingletonFactory(SchemaVersionsRepositoryFactory.class);
        bindFactory(AvroCompiledSchemaRepositoryFactory.class).in(Singleton.class).to(new TypeLiteral<CompiledSchemaRepository<Schema>>() {});
        bindSingletonFactory(SchemaRepositoryFactory.class);

        bindSingleton(CuratorClientFactory.class);
        bindSingleton(HermesMetrics.class);
        bindSingletonFactory(ConfigFactoryCreator.class);
        bindSingleton(MessageContentWrapper.class);
        bindSingleton(DeserializationMetrics.class);
        bindSingleton(JsonMessageContentWrapper.class);
        bindSingleton(AvroMessageContentWrapper.class);
        bindSingleton(AvroMessageAnySchemaVersionContentWrapper.class);
        bindSingleton(AvroMessageSchemaIdAwareContentWrapper.class);
        bindSingleton(AvroMessageHeaderSchemaVersionContentWrapper.class);
        bindSingleton(AvroMessageHeaderSchemaIdContentWrapper.class);
        bindSingleton(AvroMessageSchemaVersionTruncationContentWrapper.class);
        bind(SchemaOnlineChecksWaitingRateLimiter.class).in(Singleton.class).to(SchemaOnlineChecksRateLimiter.class);

        bindSingletonFactory(HermesCuratorClientFactory.class).named(CuratorType.HERMES);
        bindSingletonFactory(GraphiteWebTargetFactory.class);
        bindSingletonFactory(ObjectMapperFactory.class);
        bindSingletonFactory(SharedCounterFactory.class);
        bindSingletonFactory(MetricRegistryFactory.class);
        bindSingletonFactory(ZookeeperPathsFactory.class);
        bindSingletonFactory(GroupRepositoryFactory.class);
        bindSingletonFactory(TopicRepositoryFactory.class);
        bindSingletonFactory(SubscriptionRepositoryFactory.class);
        bindSingletonFactory(SubscriptionOffsetChangeIndicatorFactory.class);
        bindSingletonFactory(PathsCompilerFactory.class);
        bindSingletonFactory(KafkaNamesMapperFactory.class);
        bindSingletonFactory(MessagePreviewRepositoryFactory.class);
        bindFactory(WorkloadConstraintsRepositoryFactory.class).in(Singleton.class).to(WorkloadConstraintsRepository.class);

        bind(ZookeeperInternalNotificationBus.class).to(InternalNotificationsBus.class);
        bindSingletonFactory(ModelAwareZookeeperNotifyingCacheFactory.class);
        bindSingletonFactory(OAuthProviderRepositoryFactory.class);
    }
}
