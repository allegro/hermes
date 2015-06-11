package pl.allegro.tech.hermes.common.di;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yammer.metrics.core.HealthCheckRegistry;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.broker.ZookeeperBrokerStorage;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.di.factories.BoonObjectMapperFactory;
import pl.allegro.tech.hermes.common.di.factories.CuratorClientFactory;
import pl.allegro.tech.hermes.common.di.factories.DistributedEphemeralCounterFactory;
import pl.allegro.tech.hermes.common.di.factories.GraphiteWebTargetFactory;
import pl.allegro.tech.hermes.common.di.factories.GroupRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.HermesCuratorClientFactory;
import pl.allegro.tech.hermes.common.di.factories.KafkaCuratorClientFactory;
import pl.allegro.tech.hermes.common.di.factories.MetricRegistryFactory;
import pl.allegro.tech.hermes.common.di.factories.ObjectMapperFactory;
import pl.allegro.tech.hermes.common.di.factories.PathsCompilerFactory;
import pl.allegro.tech.hermes.common.di.factories.SharedCounterFactory;
import pl.allegro.tech.hermes.common.di.factories.SimpleConsumerPoolFactory;
import pl.allegro.tech.hermes.common.di.factories.SubscriptionOffsetChangeIndicatorFactory;
import pl.allegro.tech.hermes.common.di.factories.SubscriptionRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.TopicRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.ZookeeperPathsFactory;
import pl.allegro.tech.hermes.common.kafka.SimpleConsumerPool;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.metric.counter.zookeeper.ZookeeperCounterStorage;
import pl.allegro.tech.hermes.common.time.Clock;
import pl.allegro.tech.hermes.common.time.SystemClock;
import pl.allegro.tech.hermes.common.util.HostnameResolver;
import pl.allegro.tech.hermes.common.util.InetAddressHostnameResolver;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.subscription.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.DistributedEphemeralCounter;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter;

import javax.inject.Singleton;
import javax.ws.rs.client.WebTarget;

public class CommonBinder extends AbstractBinder {

    @Override
    protected void configure() {
        bind(CuratorClientFactory.class).to(CuratorClientFactory.class).in(Singleton.class);
        bindFactory(HermesCuratorClientFactory.class).named(CuratorType.HERMES).to(CuratorFramework.class).in(Singleton.class);
        bindFactory(KafkaCuratorClientFactory.class).named(CuratorType.KAFKA).to(CuratorFramework.class).in(Singleton.class);
        bindFactory(GraphiteWebTargetFactory.class).to(WebTarget.class).in(Singleton.class);
        bindFactory(ObjectMapperFactory.class).to(ObjectMapper.class).in(Singleton.class);
        bindFactory(BoonObjectMapperFactory.class).to(org.boon.json.ObjectMapper.class).in(Singleton.class);
        bind(HermesMetrics.class).to(HermesMetrics.class).in(Singleton.class);
        bind(HealthCheckRegistry.class).to(HealthCheckRegistry.class).in(Singleton.class);
        bind(ConfigFactory.class).to(ConfigFactory.class).in(Singleton.class);

        bindFactory(SharedCounterFactory.class).to(SharedCounter.class).in(Singleton.class);
        bindFactory(DistributedEphemeralCounterFactory.class).in(Singleton.class).to(DistributedEphemeralCounter.class);

        bind(ZookeeperCounterStorage.class).to(CounterStorage.class).in(Singleton.class);

        bind(ZookeeperBrokerStorage.class).to(BrokerStorage.class).in(Singleton.class);

        bind(SystemClock.class).to(Clock.class).in(Singleton.class);

        bindFactory(MetricRegistryFactory.class).in(Singleton.class).to(MetricRegistry.class);
        bindFactory(ZookeeperPathsFactory.class).in(Singleton.class).to(ZookeeperPaths.class);
        bindFactory(GroupRepositoryFactory.class).in(Singleton.class).to(GroupRepository.class);
        bindFactory(TopicRepositoryFactory.class).in(Singleton.class).to(TopicRepository.class);
        bindFactory(SubscriptionRepositoryFactory.class).in(Singleton.class).to(SubscriptionRepository.class);
        bind(ZookeeperBrokerStorage.class).to(BrokerStorage.class).in(Singleton.class);
        bindFactory(SimpleConsumerPoolFactory.class).in(Singleton.class).to(SimpleConsumerPool.class);
        bindFactory(SubscriptionOffsetChangeIndicatorFactory.class).in(Singleton.class).to(SubscriptionOffsetChangeIndicator.class);
        bind(InetAddressHostnameResolver.class).in(Singleton.class).to(HostnameResolver.class);
        bindFactory(PathsCompilerFactory.class).in(Singleton.class).to(PathsCompiler.class);
    }
}
