package pl.allegro.tech.hermes.consumers.consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.common.metric.SubscriptionMetrics;
import pl.allegro.tech.hermes.consumers.consumer.profiling.ConsumerProfiler;
import pl.allegro.tech.hermes.consumers.consumer.profiling.NoOpConsumerProfiler;
import pl.allegro.tech.hermes.consumers.consumer.rate.AdjustableSemaphore;
import pl.allegro.tech.hermes.consumers.consumer.rate.SerialConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.result.ErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.SuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;
import pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.NoOpConsumerNodeLoadRegistry;
import pl.allegro.tech.hermes.consumers.test.MessageBuilder;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.api.SubscriptionOAuthPolicy.passwordGrantOAuthPolicy;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

@RunWith(MockitoJUnitRunner.class)
public class ConsumerMessageSenderTest {

    public static final int ASYNC_TIMEOUT_MS = 2000;
    private final Subscription subscription = subscriptionWithTtl(10);

    private final Subscription subscriptionWith4xxRetry = subscriptionWithTtlAndClientErrorRetry(10);

    @Mock
    private MessageSender messageSender;

    @Mock
    private MessageSenderFactory messageSenderFactory;

    @Mock
    private SuccessHandler successHandler;

    @Mock
    private ErrorHandler errorHandler;

    @Mock
    private SerialConsumerRateLimiter rateLimiter;

    @Mock
    private HermesTimer consumerLatencyTimer;

    @Mock
    private HermesCounter retries;

    @Mock
    private HermesTimer rateLimiterAcquireTimer;

    @Mock
    private HermesTimerContext consumerLatencyTimerContext;

    @Mock
    private HermesTimerContext rateLimiterAcquireTimerContext;

    @Mock
    private HermesCounter failedMeter;

    @Mock
    private HermesCounter errors;

    private AdjustableSemaphore inflightSemaphore;

    private final ConsumerProfiler profiler = new NoOpConsumerProfiler();

    private ConsumerMessageSender sender;

    @Mock
    private SubscriptionMetrics subscriptionMetrics;

    @Mock
    private MetricsFacade metricsFacade;

    @Before
    public void setUp() {
        when(metricsFacade.subscriptions()).thenReturn(subscriptionMetrics);
        setUpMetrics(subscription);
        setUpMetrics(subscriptionWith4xxRetry);
        inflightSemaphore = new AdjustableSemaphore(0);
        sender = consumerMessageSender(subscription);
    }

    private void setUpMetrics(Subscription subscription) {
        when(metricsFacade.subscriptions().latency(subscription.getQualifiedName())).thenReturn(consumerLatencyTimer);
        when(metricsFacade.subscriptions().rateLimiterAcquire(subscription.getQualifiedName())).thenReturn(rateLimiterAcquireTimer);
        when(metricsFacade.subscriptions().otherErrorsCounter(subscription.getQualifiedName())).thenReturn(errors);
        when(consumerLatencyTimer.time()).thenReturn(consumerLatencyTimerContext);
        when(rateLimiterAcquireTimer.time()).thenReturn(rateLimiterAcquireTimerContext);
        when(metricsFacade.subscriptions().failuresCounter(subscription.getQualifiedName())).thenReturn(failedMeter);
        when(metricsFacade.subscriptions().retries(subscription.getQualifiedName())).thenReturn(retries);
    }

    @Test
    public void shouldHandleSuccessfulSending() {
        // given
        Message message = message();
        when(messageSender.send(message)).thenReturn(success());

        // when
        sender.sendAsync(message, profiler);
        verify(successHandler, timeout(1000)).handleSuccess(eq(message), eq(subscription), any(MessageSendingResult.class));

        // then
        verifySemaphoreReleased();
        verifyLatencyTimersCountedTimes(subscription, 1, 1);
        verifyRateLimiterAcquireTimersCountedTimes(subscription, 1, 1);
        verifyNoInteractions(errorHandler);
        verifyNoInteractions(failedMeter);
        verifyRateLimiterAcquired();
        verifyNoInteractions(retries);
    }

    @Test
    public void shouldKeepTryingToSendMessageFailedSending() {
        // given
        Message message = message();
        doReturn(failure()).doReturn(failure()).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendAsync(message, profiler);
        verify(successHandler, timeout(1000)).handleSuccess(eq(message), eq(subscription), any(MessageSendingResult.class));

        // then
        verifySemaphoreReleased();
        verifyLatencyTimersCountedTimes(subscription, 3, 3);
        verifyRateLimiterAcquireTimersCountedTimes(subscription, 3, 3);
        verifyErrorHandlerHandleFailed(message, subscription, 2);
        verifyRateLimiterAcquired(3);
        verifyRetryCounted(2);
    }

    @Test
    public void shouldDiscardMessageWhenTTLIsExceeded() {
        // given
        Message message = messageWithTimestamp(System.currentTimeMillis() - 11000);
        doReturn(failure()).when(messageSender).send(message);

        // when
        sender.sendAsync(message, profiler);

        // then
        verify(errorHandler, timeout(1000)).handleDiscarded(eq(message), eq(subscription), any(MessageSendingResult.class));
        verifySemaphoreReleased();
        verifyNoInteractions(successHandler);
        verifyLatencyTimersCountedTimes(subscription, 1, 1);
        verifyRateLimiterAcquireTimersCountedTimes(subscription, 1, 1);
        verifyRateLimiterAcquired();
        verifyNoInteractions(retries);
    }

    @Test
    public void shouldNotKeepTryingToSendMessageFailedWithStatusCode4xx() {
        // given
        Message message = message();
        doReturn(failure(403)).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendAsync(message, profiler);

        // then
        verify(errorHandler, timeout(1000)).handleDiscarded(eq(message), eq(subscription), any(MessageSendingResult.class));
        verifySemaphoreReleased();
        verifyNoInteractions(successHandler);
        verifyLatencyTimersCountedTimes(subscription, 1, 1);
        verifyRateLimiterAcquireTimersCountedTimes(subscription, 1, 1);
        verifyRateLimiterAcquired();
        verifyNoInteractions(retries);
    }

    @Test
    public void shouldKeepTryingToSendMessageFailedWithStatusCode4xxForSubscriptionWith4xxRetry() {
        // given
        final int expectedNumbersOfFailures = 3;
        ConsumerMessageSender sender = consumerMessageSender(subscriptionWith4xxRetry);
        Message message = message();
        doReturn(failure(403)).doReturn(failure(403)).doReturn(failure(403)).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendAsync(message, profiler);
        verify(successHandler, timeout(1000)).handleSuccess(eq(message), eq(subscriptionWith4xxRetry), any(MessageSendingResult.class));

        // then
        verifySemaphoreReleased();
        verify(errorHandler,
            timeout(1000).times(expectedNumbersOfFailures)).handleFailed(eq(message),
            eq(subscriptionWith4xxRetry),
            any(MessageSendingResult.class)
        );
        verifyRateLimiterAcquired(expectedNumbersOfFailures + 1);
        verifyRetryCounted(expectedNumbersOfFailures);
    }

    @Test
    public void shouldRetryOn401UnauthorizedForOAuthSecuredSubscription() {
        // given
        final int expectedNumbersOfFailures = 2;
        Subscription subscription = subscriptionWithout4xxRetryAndWithOAuthPolicy();
        setUpMetrics(subscription);
        ConsumerMessageSender sender = consumerMessageSender(subscription);
        Message message = message();
        doReturn(failure(401)).doReturn(failure(401)).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendAsync(message, profiler);

        // then
        verifyErrorHandlerHandleFailed(message, subscription, expectedNumbersOfFailures);
        verify(successHandler, timeout(1000)).handleSuccess(eq(message), eq(subscription), any(MessageSendingResult.class));
        verifyRetryCounted(expectedNumbersOfFailures);
        verifyRateLimiterAcquired(expectedNumbersOfFailures + 1);
    }

    @Test
    public void shouldBackoffRetriesWhenEndpointFails() throws InterruptedException {
        // given
        final int executionTime = 100;
        final int senderBackoffTime = 50;
        final int expectedNumberOfFailures = 1 + executionTime / senderBackoffTime;
        Subscription subscriptionWithBackoff = subscriptionWithBackoff(senderBackoffTime);
        setUpMetrics(subscriptionWithBackoff);

        sender = consumerMessageSender(subscriptionWithBackoff);
        Message message = message();
        doReturn(failure(500)).when(messageSender).send(message);

        //when
        sender.sendAsync(message, profiler);

        //then
        Thread.sleep(executionTime);
        verifyErrorHandlerHandleFailed(message, subscriptionWithBackoff, expectedNumberOfFailures);
        verifyRateLimiterAcquired(expectedNumberOfFailures);
        verifyRetryCounted(expectedNumberOfFailures);
    }

    @Test
    public void shouldNotRetryOnRetryAfterAboveTtl() {
        // given
        int retrySeconds = subscription.getSerialSubscriptionPolicy().getMessageTtl();
        Message message = messageWithTimestamp(System.currentTimeMillis() - 1);
        doReturn(backoff(retrySeconds + 1)).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendAsync(message, profiler);

        // then
        verify(errorHandler, timeout(1000)).handleDiscarded(eq(message), eq(subscription), any(MessageSendingResult.class));
        verifySemaphoreReleased();
        verifyNoInteractions(successHandler);
        verifyLatencyTimersCountedTimes(subscription, 1, 1);
        verifyRateLimiterAcquireTimersCountedTimes(subscription, 1, 1);
        verifyRateLimiterAcquired();
        verifyNoInteractions(retries);
    }

    @Test
    public void shouldDeliverToModifiedEndpoint() {
        // given
        Message message = message();
        Subscription subscriptionWithModfiedEndpoint = subscriptionWithEndpoint("http://somewhere:9876");
        MessageSender otherMessageSender = mock(MessageSender.class);

        when(messageSenderFactory.create(eq(subscriptionWithModfiedEndpoint), any(ResilientMessageSender.class)))
                .thenReturn(otherMessageSender);
        when(otherMessageSender.send(message)).thenReturn(success());

        // when
        sender.updateSubscription(subscriptionWithModfiedEndpoint);
        sender.sendAsync(message, profiler);

        // then
        verify(otherMessageSender, timeout(1000)).send(message);
        verifyRateLimiterAcquired();
        verifyNoInteractions(retries);
    }

    @Test
    public void shouldDeliverToNewSenderAfterModifiedTimeout() {
        // given
        Message message = message();
        Subscription subscriptionWithModifiedTimeout = subscriptionWithRequestTimeout(2000);
        MessageSender otherMessageSender = mock(MessageSender.class);

        when(messageSenderFactory.create(eq(subscriptionWithModifiedTimeout), any(ResilientMessageSender.class)))
                .thenReturn(otherMessageSender);
        when(otherMessageSender.send(message)).thenReturn(success());

        // when
        sender.updateSubscription(subscriptionWithModifiedTimeout);
        sender.sendAsync(message, profiler);

        // then
        verify(otherMessageSender, timeout(1000)).send(message);
        verifyRateLimiterAcquired();
        verifyNoInteractions(retries);
    }

    @Test
    public void shouldDelaySendingMessageForHalfSecond() {
        // given
        Subscription subscription = subscriptionBuilderWithTestValues()
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults()
                        .withSendingDelay(500)
                        .build())
                .build();
        setUpMetrics(subscription);

        Message message = message();
        when(messageSender.send(message)).thenReturn(success());
        ConsumerMessageSender sender = consumerMessageSender(subscription);

        // when
        long sendingStartTime = System.currentTimeMillis();
        sender.sendAsync(message, profiler);
        verify(successHandler, timeout(1000)).handleSuccess(eq(message), eq(subscription), any(MessageSendingResult.class));

        // then
        long sendingTime = System.currentTimeMillis() - sendingStartTime;
        assertThat(sendingTime).isGreaterThanOrEqualTo(500);
        verifyRateLimiterAcquired();
        verifyNoInteractions(retries);
    }

    @Test
    public void shouldCalculateSendingDelayBasingOnPublishingTimestamp() {
        // given
        Subscription subscription = subscriptionBuilderWithTestValues()
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults()
                        .withSendingDelay(2000)
                        .build())
                .build();
        setUpMetrics(subscription);

        Message message = messageWithTimestamp(System.currentTimeMillis() - 1800);
        when(messageSender.send(message)).thenReturn(success());
        ConsumerMessageSender sender = consumerMessageSender(subscription);

        // when
        long sendingStartTime = System.currentTimeMillis();
        sender.sendAsync(message, profiler);
        verify(successHandler, timeout(500)).handleSuccess(eq(message), eq(subscription), any(MessageSendingResult.class));

        // then
        long sendingTime = System.currentTimeMillis() - sendingStartTime;
        assertThat(sendingTime).isLessThan(300);
        verifyRateLimiterAcquired();
        verifyNoInteractions(retries);
    }

    @Test
    public void shouldIncreaseRetryBackoffExponentially() throws InterruptedException {
        // given
        final int expectedNumberOfFailures = 2;
        final int backoff = 500;
        final double multiplier = 2;
        Subscription subscription = subscriptionWithExponentialRetryBackoff(backoff, multiplier);
        setUpMetrics(subscription);
        Message message = message();
        doReturn(failure()).doReturn(failure()).doReturn(success()).when(messageSender).send(message);
        ConsumerMessageSender sender = consumerMessageSender(subscription);

        // when
        sender.sendAsync(message, profiler);
        Thread.sleep(backoff + (long) multiplier * backoff - 100);

        // then
        verifyNoInteractions(successHandler);
        verifyRateLimiterAcquired(expectedNumberOfFailures);
        verifyRetryCounted(expectedNumberOfFailures);
    }

    @Test
    public void shouldIgnoreExponentialRetryBackoffWithRetryAfter() {
        // given
        final int expectedNumberOfRetries = 2;
        final int retrySeconds = 1;
        final int backoff = 5000;
        final double multiplier = 3;
        Subscription subscription = subscriptionWithExponentialRetryBackoff(backoff, multiplier);
        setUpMetrics(subscription);
        Message message = message();
        doReturn(backoff(retrySeconds)).doReturn(backoff(retrySeconds)).doReturn(success()).when(messageSender).send(message);
        ConsumerMessageSender sender = consumerMessageSender(subscription);

        // when
        sender.sendAsync(message, profiler);

        //then
        verify(successHandler, timeout(retrySeconds * 1000 * 2 + 500))
            .handleSuccess(eq(message), eq(subscription), any(MessageSendingResult.class));
        verifyRateLimiterAcquired(expectedNumberOfRetries + 1);
        verifyRetryCounted(expectedNumberOfRetries);
    }

    @Test
    public void shouldIgnoreExponentialRetryBackoffAfterExceededTtl() throws InterruptedException {
        final int backoff = 1000;
        final double multiplier = 2;
        final int ttl = 2;
        Subscription subscription = subscriptionWithExponentialRetryBackoff(backoff, multiplier, ttl);
        setUpMetrics(subscription);
        Message message = message();
        doReturn(failure()).doReturn(failure()).doReturn(success()).when(messageSender).send(message);
        ConsumerMessageSender sender = consumerMessageSender(subscription);

        // when
        sender.sendAsync(message, profiler);
        Thread.sleep(backoff + (long) multiplier * backoff + 1000);

        //then
        verifyNoInteractions(successHandler);
        verifyRateLimiterAcquired(2);
        verifyRetryCounted();
    }

    private ConsumerMessageSender consumerMessageSender(Subscription subscription) {
        when(messageSenderFactory.create(eq(subscription), any(ResilientMessageSender.class))).thenReturn(messageSender);
        ConsumerMessageSender sender = new ConsumerMessageSender(
                subscription,
                messageSenderFactory,
                List.of(successHandler),
                List.of(errorHandler),
                rateLimiter,
                Executors.newSingleThreadExecutor(),
                () -> inflightSemaphore.release(),
                metricsFacade,
                ASYNC_TIMEOUT_MS,
                new FutureAsyncTimeout(Executors.newSingleThreadScheduledExecutor()),
                Clock.systemUTC(),
                new NoOpConsumerNodeLoadRegistry().register(subscription.getQualifiedName())
        );
        sender.initialize();

        return sender;
    }

    private void verifyErrorHandlerHandleFailed(Message message, Subscription subscription, int times) {
        verifyErrorHandlerHandleFailed(message, subscription, times, 1000);
    }

    private void verifyErrorHandlerHandleFailed(Message message, Subscription subscription, int times, int timeout) {
        verify(errorHandler, timeout(timeout).times(times)).handleFailed(eq(message), eq(subscription), any(MessageSendingResult.class));
    }

    private void verifyLatencyTimersCountedTimes(Subscription subscription, int timeCount, int closeCount) {
        verify(metricsFacade.subscriptions(), times(1)).latency(subscription.getQualifiedName());
        verify(consumerLatencyTimer, times(timeCount)).time();
        verify(consumerLatencyTimerContext, times(closeCount)).close();
    }

    private void verifyRateLimiterAcquireTimersCountedTimes(Subscription subscription, int timeCount, int closeCount) {
        verify(metricsFacade.subscriptions(), times(1)).rateLimiterAcquire(subscription.getQualifiedName());
        verify(rateLimiterAcquireTimer, times(timeCount)).time();
        verify(rateLimiterAcquireTimerContext, times(closeCount)).close();
    }

    private Subscription subscriptionWithTtl(int ttl) {
        return subscriptionBuilderWithTestValues()
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults()
                        .withMessageTtl(ttl)
                        .build())
                .build();
    }

    private Subscription subscriptionWithTtlAndClientErrorRetry(int ttl) {
        return subscriptionBuilderWithTestValues()
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults()
                        .withMessageTtl(ttl)
                        .withClientErrorRetry()
                        .build())
                .build();
    }

    private Subscription subscriptionWithBackoff(int backoff) {
        return subscriptionBuilderWithTestValues()
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults()
                        .withMessageBackoff(backoff)
                        .build())
                .build();
    }

    private Subscription subscriptionWithout4xxRetryAndWithOAuthPolicy() {
        return subscriptionBuilderWithTestValues()
                .withOAuthPolicy(passwordGrantOAuthPolicy("myOAuthProvider")
                        .withUsername("user1")
                        .withPassword("abc123")
                        .build())
                .build();
    }

    private Subscription subscriptionWithEndpoint(String endpoint) {
        return subscriptionBuilderWithTestValues().withEndpoint(endpoint).build();
    }

    private Subscription subscriptionWithRequestTimeout(int timeout) {
        return subscriptionBuilderWithTestValues().withRequestTimeout(timeout).build();
    }

    private Subscription subscriptionWithExponentialRetryBackoff(int messageBackoff, double backoffMultiplier) {
        return subscriptionWithExponentialRetryBackoff(messageBackoff, backoffMultiplier, 3600);
    }

    private Subscription subscriptionWithExponentialRetryBackoff(int messageBackoff, double backoffMultiplier, int ttl) {
        return subscriptionBuilderWithTestValues()
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults()
                        .withMessageBackoff(messageBackoff)
                        .withBackoffMultiplier(backoffMultiplier)
                        .withMessageTtl(ttl)
                        .build())
                .build();
    }

    private SubscriptionBuilder subscriptionBuilderWithTestValues() {
        return subscription("group.topic", "subscription");
    }

    private RuntimeException exception() {
        return new RuntimeException("problem");
    }

    private CompletableFuture<MessageSendingResult> success() {
        return CompletableFuture.completedFuture(MessageSendingResult.succeededResult());
    }

    private CompletableFuture<MessageSendingResult> failure() {
        return CompletableFuture.completedFuture(MessageSendingResult.failedResult(exception()));
    }

    private CompletableFuture<MessageSendingResult> failure(int statusCode) {
        return CompletableFuture.completedFuture(MessageSendingResult.failedResult(statusCode));
    }

    private CompletableFuture<MessageSendingResult> backoff(int seconds) {
        return CompletableFuture.completedFuture(MessageSendingResult.retryAfter(seconds));
    }

    private void verifySemaphoreReleased() {
        assertThat(inflightSemaphore.availablePermits()).isEqualTo(1);
    }

    private void verifyRateLimiterAcquired() {
        verifyRateLimiterAcquired(1);
    }

    private void verifyRateLimiterAcquired(int times) {
        verify(rateLimiter, times(times)).acquire();
    }

    private void verifyRetryCounted() {
        verifyRetryCounted(1);
    }

    private void verifyRetryCounted(int times) {
        verify(retries, times(times)).increment();
    }

    private Message message() {
        return messageWithTimestamp(System.currentTimeMillis());
    }

    private Message messageWithTimestamp(long timestamp) {
        return MessageBuilder
                .withTestMessage()
                .withContent("{\"username\":\"ala\"}", StandardCharsets.UTF_8)
                .withReadingTimestamp(timestamp)
                .withPublishingTimestamp(timestamp)
                .build();
    }
}
