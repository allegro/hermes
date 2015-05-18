package pl.allegro.tech.hermes.consumers.supervisor;

import com.yammer.metrics.core.Clock;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.offset.PartitionOffsetHelper;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimitSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculator;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageSplitter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.SplitMessagesReceiver;
import pl.allegro.tech.hermes.consumers.consumer.result.DefaultErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.DefaultSuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.ErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.SuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.message.tracker.Trackers;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ConsumerFactory {

    private final ConsumerRateLimitSupervisor consumerRateLimitSupervisor;
    private final OutputRateCalculator outputRateCalculator;
    private final ReceiverFactory messageReceiverFactory;
    private final MessageSenderFactory messageSenderFactory;
    private final HermesMetrics hermesMetrics;
    private final MessageSplitter messageSplitter;
    private final ConfigFactory configFactory;
    private final UndeliveredMessageLog undeliveredMessageLog;
    private final Trackers trackers;
    private final ExecutorService rateLimiterReportingExecutor;

    @Inject
    public ConsumerFactory(ReceiverFactory messageReceiverFactory,
            MessageSenderFactory messageSenderFactory,
            HermesMetrics hermesMetrics,
            MessageSplitter messageSplitter,
            ConfigFactory configFactory,
            UndeliveredMessageLog undeliveredMessageLog,
            ConsumerRateLimitSupervisor consumerRateLimitSupervisor,
            OutputRateCalculator outputRateCalculator,
            Trackers trackers) {
        this.messageReceiverFactory = messageReceiverFactory;
        this.messageSenderFactory = messageSenderFactory;
        this.hermesMetrics = hermesMetrics;
        this.messageSplitter = messageSplitter;
        this.configFactory = configFactory;
        this.undeliveredMessageLog = undeliveredMessageLog;
        this.consumerRateLimitSupervisor = consumerRateLimitSupervisor;
        this.outputRateCalculator = outputRateCalculator;
        this.trackers = trackers;
        this.rateLimiterReportingExecutor
                = Executors.newFixedThreadPool(configFactory.getIntProperty(Configs.CONSUMER_RATE_LIMITER_REPORTING_THREAD_POOL_SIZE));
    }

    Consumer createConsumer(Subscription subscription) {
        MessageReceiver messageReceiver = messageReceiverFactory.createMessageReceiver(subscription);

        PartitionOffsetHelper partitionOffsetHelper
                = new PartitionOffsetHelper(toMillis(configFactory.getIntProperty(Configs.CONSUMER_COMMIT_OFFSET_CACHE_EXPIRATION)));
        ConsumerRateLimiter consumerRateLimiter = new ConsumerRateLimiter(subscription, outputRateCalculator, hermesMetrics,
                consumerRateLimitSupervisor);
        MessageSender messageSender = messageSenderFactory.create(subscription);
        SuccessHandler successHandler = new DefaultSuccessHandler(partitionOffsetHelper, hermesMetrics, trackers);
        ErrorHandler errorHandler = new DefaultErrorHandler(partitionOffsetHelper, hermesMetrics, undeliveredMessageLog,
                Clock.defaultClock(), trackers, configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME));

        SplitMessagesReceiver splitMessagesReceiver = new SplitMessagesReceiver(messageReceiver, messageSplitter);

        Semaphore inflightSemaphore = new Semaphore(configFactory.getIntProperty(Configs.CONSUMER_INFLIGHT_SIZE));

        ConsumerMessageSender sender = new ConsumerMessageSender(subscription, messageSender, successHandler, errorHandler,
                consumerRateLimiter, rateLimiterReportingExecutor, inflightSemaphore, hermesMetrics,
                configFactory.getIntProperty(Configs.CONSUMER_SENDER_ASYNC_TIMEOUT_MS));

        return new Consumer(splitMessagesReceiver, hermesMetrics, subscription, consumerRateLimiter,
                partitionOffsetHelper, sender, inflightSemaphore, trackers);
    }

    private long toMillis(int seconds) {
        return TimeUnit.MILLISECONDS.convert(seconds, TimeUnit.SECONDS);
    }
}
