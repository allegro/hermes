package pl.allegro.tech.hermes.common.di;

import com.github.fge.jsonschema.main.JsonSchema;
import com.yammer.metrics.core.HealthCheckRegistry;
import org.apache.avro.Schema;
import org.glassfish.hk2.api.TypeLiteral;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.broker.ZookeeperBrokerStorage;
import pl.allegro.tech.hermes.common.clock.ClockFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.di.factories.BoonObjectMapperFactory;
import pl.allegro.tech.hermes.common.di.factories.CuratorClientFactory;
import pl.allegro.tech.hermes.common.di.factories.DistributedEphemeralCounterFactory;
import pl.allegro.tech.hermes.common.di.factories.GraphiteWebTargetFactory;
import pl.allegro.tech.hermes.common.di.factories.GroupRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.HermesCuratorClientFactory;
import pl.allegro.tech.hermes.common.di.factories.KafkaCuratorClientFactory;
import pl.allegro.tech.hermes.common.di.factories.MessagePreviewRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.MetricRegistryFactory;
import pl.allegro.tech.hermes.common.di.factories.ModelAwareZookeeperNotifyingCacheFactory;
import pl.allegro.tech.hermes.common.di.factories.OAuthProviderRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.ObjectMapperFactory;
import pl.allegro.tech.hermes.common.di.factories.PathsCompilerFactory;
import pl.allegro.tech.hermes.common.di.factories.SharedCounterFactory;
import pl.allegro.tech.hermes.common.di.factories.SimpleConsumerPoolFactory;
import pl.allegro.tech.hermes.common.di.factories.SubscriptionOffsetChangeIndicatorFactory;
import pl.allegro.tech.hermes.common.di.factories.SubscriptionRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.TopicRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.ZookeeperPathsFactory;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapperFactory;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.metric.counter.zookeeper.ZookeeperCounterStorage;
import pl.allegro.tech.hermes.common.util.HostnameResolver;
import pl.allegro.tech.hermes.common.util.InetAddressHostnameResolver;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.topic.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.infrastructure.schema.AvroCompiledSchemaRepositoryFactory;
import pl.allegro.tech.hermes.infrastructure.schema.JsonCompiledSchemaRepositoryFactory;
import pl.allegro.tech.hermes.infrastructure.schema.SchemaSourceProviderFactory;
import pl.allegro.tech.hermes.infrastructure.schema.SchemaVersionsRepositoryFactory;
import pl.allegro.tech.hermes.infrastructure.schema.repo.SchemaRepoClientFactory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.notifications.ZookeeperInternalNotificationBus;

import javax.inject.Singleton;
import java.time.Clock;

public class CommonBinder extends AbstractBinder {

    @Override
    protected void configure() {
        bind(ZookeeperCounterStorage.class).to(CounterStorage.class).in(Singleton.class);
        bind(ZookeeperBrokerStorage.class).to(BrokerStorage.class).in(Singleton.class);
        bindFactory(ClockFactory.class).in(Singleton.class).to(Clock.class);
        bind(ZookeeperBrokerStorage.class).to(BrokerStorage.class).in(Singleton.class);
        bind(InetAddressHostnameResolver.class).in(Singleton.class).to(HostnameResolver.class);
        bindSingletonFactory(SchemaSourceProviderFactory.class);
        bindSingletonFactory(SchemaRepoClientFactory.class);
        bindSingletonFactory(SchemaVersionsRepositoryFactory.class);
        bindSingleton(SchemaRepository.class);
        bindFactory(JsonCompiledSchemaRepositoryFactory.class).in(Singleton.class).to(new TypeLiteral<CompiledSchemaRepository<JsonSchema>>() {});
        bindFactory(AvroCompiledSchemaRepositoryFactory.class).in(Singleton.class).to(new TypeLiteral<CompiledSchemaRepository<Schema>>() {});

        bindSingleton(CuratorClientFactory.class);
        bindSingleton(HermesMetrics.class);
        bindSingleton(HealthCheckRegistry.class);
        bindSingleton(ConfigFactory.class);
        bindSingleton(MessageContentWrapper.class);
        bindSingleton(JsonMessageContentWrapper.class);
        bindSingleton(AvroMessageContentWrapper.class);

        bindSingletonFactory(HermesCuratorClientFactory.class).named(CuratorType.HERMES);
        bindSingletonFactory(KafkaCuratorClientFactory.class).named(CuratorType.KAFKA);
        bindSingletonFactory(GraphiteWebTargetFactory.class);
        bindSingletonFactory(ObjectMapperFactory.class);
        bindSingletonFactory(BoonObjectMapperFactory.class);
        bindSingletonFactory(SharedCounterFactory.class);
        bindSingletonFactory(DistributedEphemeralCounterFactory.class);
        bindSingletonFactory(MetricRegistryFactory.class);
        bindSingletonFactory(ZookeeperPathsFactory.class);
        bindSingletonFactory(GroupRepositoryFactory.class);
        bindSingletonFactory(TopicRepositoryFactory.class);
        bindSingletonFactory(SubscriptionRepositoryFactory.class);
        bindSingletonFactory(SimpleConsumerPoolFactory.class);
        bindSingletonFactory(SubscriptionOffsetChangeIndicatorFactory.class);
        bindSingletonFactory(PathsCompilerFactory.class);
        bindSingletonFactory(KafkaNamesMapperFactory.class);
        bindSingletonFactory(MessagePreviewRepositoryFactory.class);

        bind(ZookeeperInternalNotificationBus.class).to(InternalNotificationsBus.class);
        bindSingletonFactory(ModelAwareZookeeperNotifyingCacheFactory.class);
        bindSingletonFactory(OAuthProviderRepositoryFactory.class);
    }
}
