package pl.allegro.tech.hermes.common.di;

import com.yammer.metrics.core.HealthCheckRegistry;
import org.apache.avro.Schema;
import org.glassfish.hk2.api.TypeLiteral;
import pl.allegro.tech.hermes.common.clock.ClockFactory;
import pl.allegro.tech.hermes.common.di.factories.*;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapperFactory;
import pl.allegro.tech.hermes.common.message.wrapper.*;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.metric.counter.zookeeper.ZookeeperCounterStorage;
import pl.allegro.tech.hermes.common.schema.*;
import pl.allegro.tech.hermes.common.util.HostnameResolver;
import pl.allegro.tech.hermes.common.util.InetAddressHostnameResolver;
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
        bind(InetAddressHostnameResolver.class).in(Singleton.class).to(HostnameResolver.class);
        bindSingletonFactory(SchemaRepositoryClientFactory.class);
        bindSingletonFactory(SchemaRepositoryInstanceResolverFactory.class);
        bindSingletonFactory(RawSchemaClientFactory.class);
        bindSingletonFactory(SchemaVersionsRepositoryFactory.class);
        bindFactory(AvroCompiledSchemaRepositoryFactory.class).in(Singleton.class).to(new TypeLiteral<CompiledSchemaRepository<Schema>>() {});
        bindSingletonFactory(SchemaRepositoryFactory.class);

        bindSingleton(CuratorClientFactory.class);
        bindSingleton(HermesMetrics.class);
        bindSingleton(HealthCheckRegistry.class);
        bindSingletonFactory(ConfigFactoryCreator.class);
        bindSingleton(MessageContentWrapper.class);
        bindSingleton(DeserializationMetrics.class);
        bindSingleton(JsonMessageContentWrapper.class);
        bindSingleton(AvroMessageContentWrapper.class);
        bindSingleton(AvroMessageAnySchemaVersionContentWrapper.class);
        bindSingleton(AvroMessageSchemaIdAwareContentWrapper.class);
        bindSingleton(AvroMessageHeaderSchemaVersionContentWrapper.class);
        bindSingleton(AvroMessageHeaderSchemaIdContentWrapper.class);
        bind(SchemaOnlineChecksWaitingRateLimiter.class).in(Singleton.class).to(SchemaOnlineChecksRateLimiter.class);

        bindSingletonFactory(HermesCuratorClientFactory.class).named(CuratorType.HERMES);
        bindSingletonFactory(KafkaCuratorClientFactory.class).named(CuratorType.KAFKA);
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
