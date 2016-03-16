package pl.allegro.tech.hermes.consumers.consumer;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.result.DefaultErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.DefaultSuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.ErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.SuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import javax.inject.Inject;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_RATE_LIMITER_REPORTING_THREAD_POOL_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SENDER_ASYNC_TIMEOUT_MS;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;

public class ConsumerMessageSenderFactory {

    private final ConfigFactory configFactory;
    private final HermesMetrics hermesMetrics;
    private final MessageSenderFactory messageSenderFactory;
    private final Trackers trackers;
    private final FutureAsyncTimeout<MessageSendingResult> futureAsyncTimeout;
    private final UndeliveredMessageLog undeliveredMessageLog;
    private final Clock clock;
    private final ExecutorService rateLimiterReportingExecutor;

    @Inject
    public ConsumerMessageSenderFactory(ConfigFactory configFactory, HermesMetrics hermesMetrics, MessageSenderFactory messageSenderFactory,
                                        Trackers trackers, FutureAsyncTimeout<MessageSendingResult> futureAsyncTimeout,
                                        UndeliveredMessageLog undeliveredMessageLog, Clock clock, InstrumentedExecutorServiceFactory instrumentedExecutorServiceFactory) {

        this.configFactory = configFactory;
        this.hermesMetrics = hermesMetrics;
        this.messageSenderFactory = messageSenderFactory;
        this.trackers = trackers;
        this.futureAsyncTimeout = futureAsyncTimeout;
        this.undeliveredMessageLog = undeliveredMessageLog;
        this.clock = clock;
        this.rateLimiterReportingExecutor = instrumentedExecutorServiceFactory.getExecutorService("rate-limiter-reporter", configFactory.getIntProperty(CONSUMER_RATE_LIMITER_REPORTING_THREAD_POOL_SIZE),
                configFactory.getBooleanProperty(Configs.CONSUMER_RATE_LIMITER_REPORTING_THREAD_POOL_MONITORING));
    }

    public ConsumerMessageSender create(Subscription subscription, ConsumerRateLimiter consumerRateLimiter,
                                        SubscriptionOffsetCommitQueues subscriptionOffsetCommitQueues, Semaphore inflightSemaphore) {

        SuccessHandler successHandler = new DefaultSuccessHandler(subscriptionOffsetCommitQueues, hermesMetrics, trackers);
        ErrorHandler errorHandler = new DefaultErrorHandler(subscriptionOffsetCommitQueues, hermesMetrics, undeliveredMessageLog,
                clock, trackers, configFactory.getStringProperty(KAFKA_CLUSTER_NAME));

        return new ConsumerMessageSender(subscription,
                messageSenderFactory,
                successHandler,
                errorHandler,
                consumerRateLimiter,
                rateLimiterReportingExecutor,
                inflightSemaphore,
                hermesMetrics,
                configFactory.getIntProperty(CONSUMER_SENDER_ASYNC_TIMEOUT_MS),
                futureAsyncTimeout);
    }

}
