package pl.allegro.tech.hermes.consumers.supervisor;

import com.google.common.collect.ImmutableMap;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsStorage;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.message.undelivered.UndeliveredMessageLogPersister;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;

public class ConsumersSupervisorFactory implements Factory<ConsumersSupervisor> {
    private final ConfigFactory configs;
    private final Map<String, Provider<ConsumersSupervisor>> availableImplementations;

    public final static String LEGACY_SUPERVISOR = "legacy";
    public final static String BACKGROUND_SUPERVISOR = "background";

    @Inject
    public ConsumersSupervisorFactory(ConfigFactory configs,
                                      SubscriptionRepository subscriptionRepository,
                                      TopicRepository topicRepository,
                                      SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator,
                                      ConsumersExecutorService executor,
                                      ConsumerFactory consumerFactory,
                                      List<MessageCommitter> messageCommitters,
                                      List<OffsetsStorage> offsetsStorages,
                                      HermesMetrics hermesMetrics,
                                      UndeliveredMessageLogPersister undeliveredMessageLogPersister,
                                      Clock clock) {

        this.configs = configs;
        this.availableImplementations = ImmutableMap.of(
                LEGACY_SUPERVISOR, () -> new LegacyConsumersSupervisor(configs,
                        subscriptionRepository, topicRepository, subscriptionOffsetChangeIndicator,
                        executor, consumerFactory, messageCommitters, offsetsStorages, hermesMetrics, undeliveredMessageLogPersister),
                BACKGROUND_SUPERVISOR, () -> new BackgroundConsumersSupervisor(configs,
                        subscriptionOffsetChangeIndicator, executor,
                        consumerFactory, messageCommitters, offsetsStorages, hermesMetrics, undeliveredMessageLogPersister, subscriptionRepository, clock));
    }

    @Override
    public ConsumersSupervisor provide() {
        return ofNullable(availableImplementations.get(configs.getStringProperty(Configs.CONSUMER_SUPERVISOR_TYPE)))
                .orElseThrow(IllegalStateException::new).get();
    }

    @Override
    public void dispose(ConsumersSupervisor instance) {

    }
}
