package pl.allegro.tech.hermes.frontend.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.event.ExecutionAttemptedEvent;
import net.jodah.failsafe.function.CheckedConsumer;
import org.glassfish.hk2.api.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.hook.Hook;
import pl.allegro.tech.hermes.common.hook.ServiceAwareHook;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducer;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducingException;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducingResults;
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageIdGenerator;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.Charset.defaultCharset;

public class MessagesPublishingStartupValidationHook implements ServiceAwareHook {

    private static final Logger logger = LoggerFactory.getLogger(MessagesPublishingStartupValidationHook.class);
    private final TopicsCache topicsCache;
    private final MessageContentWrapper messageContentWrapper;
    private final Clock clock;
    private final ConfigFactory config;
    private final BrokerMessagesProducer producer;
    private final ScheduledExecutorService scheduler;

    @Inject
    public MessagesPublishingStartupValidationHook(ConfigFactory config,
                                                   BrokerMessagesProducer brokerMessagesProducer,
                                                   MessageContentWrapper messageContentWrapper,
                                                   Clock clock,
                                                   TopicsCache topicsCache) {
        this.scheduler = Executors.newScheduledThreadPool(
                2, new ThreadFactoryBuilder().setNameFormat("messages-publishing-startup-validation-%d").build()
        );
        this.config = config;
        this.messageContentWrapper = messageContentWrapper;
        this.clock = clock;
        this.topicsCache = topicsCache;
        this.producer = brokerMessagesProducer;
    }

    @Override
    public void accept(ServiceLocator serviceLocator) {
        logger.info("Validating publishing messages to broker is available");
        Duration delay = Duration.of(config.getLongProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_RETRY_INTERVAL), ChronoUnit.MILLIS);
        Duration timeout = Duration.of(config.getLongProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_TIMEOUT_MS), ChronoUnit.MILLIS);
        RetryPolicy<BrokerMessagesProducingResults> retryPolicy = retryPolicy(delay, timeout);
        Fallback<BrokerMessagesProducingResults> fallback = fallback();
        Failsafe.with(fallback, retryPolicy)
                .with(scheduler)
                .get(context -> {
                    logger.info("Trying to publish messages, attempt:{}", context.getAttemptCount());
                    return publishMessagesToBroker();
                });
    }

    private BrokerMessagesProducingResults publishMessagesToBroker() {
        return topicsCache
                .getTopic(config.getStringProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_TOPIC_NAME))
                .map(it -> producer.publishMessages(
                        it,
                        validationMessages(config.getIntProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_MESSAGES_COUNT)),
                        config.getLongProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_TIMEOUT_MS)
                ))
                .orElseThrow(() -> new IllegalStateException("Missing topic to validate publishing messages on startup"));
    }

    private Fallback<BrokerMessagesProducingResults> fallback() {
        return Fallback.of((CheckedConsumer<ExecutionAttemptedEvent<? extends BrokerMessagesProducingResults>>) event -> {
            throw new PublishingStartupValidationException(event.getLastResult());
        })
                .handle(BrokerMessagesProducingException.class)
                .handleIf((validationResults, throwable) -> validationResults.isFailure());
    }

    private RetryPolicy<BrokerMessagesProducingResults> retryPolicy(Duration delay, Duration maxDuration) {
        return new RetryPolicy<BrokerMessagesProducingResults>()
                .handle(BrokerMessagesProducingException.class)
                .withMaxRetries(-1)
                .withDelay(delay)
                .withMaxDuration(maxDuration)
                .handleIf((result, t) -> result.isFailure());
    }

    private List<Message> validationMessages(int number) {
        return IntStream.range(0, number).mapToObj(it -> {
                    String messageId = MessageIdGenerator.generate();
                    long timestamp = clock.millis();
                    byte[] messageContent = messageContentWrapper.wrapJson("validation-message".getBytes(defaultCharset()), messageId, timestamp, Collections.emptyMap());
                    return new JsonMessage(messageId, messageContent, timestamp, null);
                }
        ).collect(Collectors.toList());
    }

    @Override
    public int getPriority() {
        return Hook.HIGHER_PRIORITY;
    }
}
