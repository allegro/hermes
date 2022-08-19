package pl.allegro.tech.hermes.consumers.consumer;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.rate.InflightsPool;
import pl.allegro.tech.hermes.consumers.consumer.rate.SerialConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.result.DefaultErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.DefaultSuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.ErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.SuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ConsumerMessageSenderFactory {

    private final String kafkaClusterName;
    private final HermesMetrics hermesMetrics;
    private final MessageSenderFactory messageSenderFactory;
    private final Trackers trackers;
    private final FutureAsyncTimeout<MessageSendingResult> futureAsyncTimeout;
    private final UndeliveredMessageLog undeliveredMessageLog;
    private final Clock clock;
    private final ConsumerAuthorizationHandler consumerAuthorizationHandler;
    private final ExecutorService rateLimiterReportingExecutor;
    private final int senderAsyncTimeoutMs;

    public ConsumerMessageSenderFactory(String kafkaClusterName, HermesMetrics hermesMetrics, MessageSenderFactory messageSenderFactory,
                                        Trackers trackers, FutureAsyncTimeout<MessageSendingResult> futureAsyncTimeout,
                                        UndeliveredMessageLog undeliveredMessageLog, Clock clock,
                                        InstrumentedExecutorServiceFactory instrumentedExecutorServiceFactory,
                                        ConsumerAuthorizationHandler consumerAuthorizationHandler,
                                        int senderAsyncTimeoutMs,
                                        int rateLimiterReportingThreadPoolSize,
                                        boolean rateLimiterReportingThreadMonitoringEnabled) {

        this.kafkaClusterName = kafkaClusterName;
        this.hermesMetrics = hermesMetrics;
        this.messageSenderFactory = messageSenderFactory;
        this.trackers = trackers;
        this.futureAsyncTimeout = futureAsyncTimeout;
        this.undeliveredMessageLog = undeliveredMessageLog;
        this.clock = clock;
        this.consumerAuthorizationHandler = consumerAuthorizationHandler;
        this.rateLimiterReportingExecutor = instrumentedExecutorServiceFactory.getExecutorService(
                "rate-limiter-reporter", rateLimiterReportingThreadPoolSize,
                rateLimiterReportingThreadMonitoringEnabled);
        this.senderAsyncTimeoutMs = senderAsyncTimeoutMs;
    }

    public ConsumerMessageSender create(Subscription subscription,
                                        SerialConsumerRateLimiter consumerRateLimiter,
                                        OffsetQueue offsetQueue,
                                        InflightsPool inflight,
                                        SubscriptionLoadRecorder subscriptionLoadRecorder) {

        List<SuccessHandler> successHandlers = Arrays.asList(
                consumerAuthorizationHandler,
                new DefaultSuccessHandler(offsetQueue, hermesMetrics, trackers));

        List<ErrorHandler> errorHandlers = Arrays.asList(
                consumerAuthorizationHandler,
                new DefaultErrorHandler(offsetQueue, hermesMetrics, undeliveredMessageLog, clock, trackers, kafkaClusterName));

        return new ConsumerMessageSender(subscription,
                messageSenderFactory,
                successHandlers,
                errorHandlers,
                consumerRateLimiter,
                rateLimiterReportingExecutor,
                inflight,
                hermesMetrics,
                senderAsyncTimeoutMs,
                futureAsyncTimeout,
                clock,
                subscriptionLoadRecorder
        );
    }

}
