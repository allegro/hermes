package pl.allegro.tech.hermes.frontend.di;

import org.I0Itec.zkclient.ZkClient;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.cache.topic.zookeeper.ZookeeperTopicsCacheFactory;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaBrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaMessageProducerFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.Producers;
import pl.allegro.tech.hermes.frontend.publishing.MessagePublisher;
import pl.allegro.tech.hermes.frontend.publishing.PublishingServlet;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.frontend.services.HealthCheckService;
import pl.allegro.tech.hermes.frontend.validator.MessageValidators;
import pl.allegro.tech.hermes.frontend.validator.TopicMessageValidatorFactory;
import pl.allegro.tech.hermes.frontend.zk.ZkClientFactory;
import pl.allegro.tech.hermes.tracker.frontend.NoOperationPublishingTracker;
import pl.allegro.tech.hermes.tracker.frontend.PublishingMessageTracker;

import javax.inject.Singleton;

public class FrontendBinder extends AbstractBinder {

    @Override
    protected void configure() {
        bind(HermesServer.class).to(HermesServer.class).in(Singleton.class);
        bind(PublishingServlet.class).to(PublishingServlet.class).in(Singleton.class);
        bind(KafkaBrokerMessageProducer.class).to(BrokerMessageProducer.class).in(Singleton.class);
        bind(MessageValidators.class).to(MessageValidators.class).in(Singleton.class);

        bind(HealthCheckService.class).to(HealthCheckService.class).in(Singleton.class);

        bindFactory(KafkaMessageProducerFactory.class).to(Producers.class).in(Singleton.class);
        bindFactory(ZkClientFactory.class).to(ZkClient.class).in(Singleton.class);
        bindSingleton(PublishingMessageTracker.class);
        bindSingleton(TopicMessageValidatorFactory.class);
        bindSingleton(NoOperationPublishingTracker.class);
        bindFactory(ZookeeperTopicsCacheFactory.class).to(TopicsCache.class).in(Singleton.class);
        bindSingleton(MessagePublisher.class);
    }

    private <T> void bindSingleton(Class<T> clazz) {
        bind(clazz).in(Singleton.class).to(clazz);
    }
}
