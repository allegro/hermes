package pl.allegro.tech.hermes.common.di;

import com.github.fge.jsonschema.main.JsonSchema;
import com.yammer.metrics.core.HealthCheckRegistry;
import org.apache.avro.Schema;
import org.glassfish.hk2.api.TypeLiteral;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.broker.ZookeeperBrokerStorage;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.di.factories.*;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.metric.counter.zookeeper.ZookeeperCounterStorage;
import pl.allegro.tech.hermes.domain.topic.schema.*;
import pl.allegro.tech.hermes.common.time.Clock;
import pl.allegro.tech.hermes.common.time.SystemClock;
import pl.allegro.tech.hermes.common.util.HostnameResolver;
import pl.allegro.tech.hermes.common.util.InetAddressHostnameResolver;

import javax.inject.Singleton;

public class CommonBinder extends AbstractBinder {

    @Override
    protected void configure() {
        bind(ZookeeperCounterStorage.class).to(CounterStorage.class).in(Singleton.class);
        bind(ZookeeperBrokerStorage.class).to(BrokerStorage.class).in(Singleton.class);
        bind(SystemClock.class).to(Clock.class).in(Singleton.class);
        bind(ZookeeperBrokerStorage.class).to(BrokerStorage.class).in(Singleton.class);
        bind(InetAddressHostnameResolver.class).in(Singleton.class).to(HostnameResolver.class);
        bind(TopicFieldSchemaSourceProvider.class).to(SchemaSourceProvider.class).in(Singleton.class);
        bindFactory(JsonSchemaRepositoryFactory.class).in(Singleton.class).to(new TypeLiteral<SchemaRepository<JsonSchema>>() {});
        bindFactory(AvroSchemaRepositoryFactory.class).in(Singleton.class).to(new TypeLiteral<SchemaRepository<Schema>>() {});
        bindSingletonFactory(SchemaRepoClientFactory.class);

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
    }
}
