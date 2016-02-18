package pl.allegro.tech.hermes.consumers.consumer;

import com.codahale.metrics.Meter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;
import pl.allegro.tech.hermes.common.metric.timer.ConsumerLatencyTimer;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.result.ErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.SuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;
import pl.allegro.tech.hermes.consumers.test.MessageBuilder;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult;
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.succeededResult;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

@RunWith(MockitoJUnitRunner.class)
public class ConsumerMessageSenderTest {

    public static final int ASYNC_TIMEOUT_MS = 2000;
    private Subscription subscription = subscriptionWithTtl(10);

    private Subscription subscriptionWith4xxRetry = subscriptionWithTtlAndClientErrorRetry(10);

    @Mock
    private MessageSender messageSender;

    @Mock
    private SuccessHandler successHandler;

    @Mock
    private ErrorHandler errorHandler;

    @Mock
    private ConsumerRateLimiter rateLimiter;

    @Mock
    private HermesMetrics hermesMetrics;

    @Mock
    private ConsumerLatencyTimer consumerLatencyTimer;

    @Mock
    private ConsumerLatencyTimer.Context consumerLatencyTimerContext;

    @Mock
    private Meter failedMeter;

    @Mock
    private Meter errors;

    private Semaphore inflightSemaphore;

    private ConsumerMessageSender sender;

    @Before
    public void setUp() {
        setUpMetrics(subscription);
        setUpMetrics(subscriptionWith4xxRetry);
        inflightSemaphore = new Semaphore(0);
        sender = consumerMessageSender(subscription);
    }

    private void setUpMetrics(Subscription subscription) {
        when(hermesMetrics.latencyTimer(subscription)).thenReturn(consumerLatencyTimer);
        when(hermesMetrics.consumerErrorsOtherMeter(subscription)).thenReturn(errors);
        when(consumerLatencyTimer.time()).thenReturn(consumerLatencyTimerContext);
        when(hermesMetrics.meter(Meters.FAILED_METER_SUBSCRIPTION, subscription.getTopicName(), subscription.getName())).thenReturn(failedMeter);
    }

    @Test
    public void shouldHandleSuccessfulSending() {
        // given
        Message message = message();
        when(messageSender.send(message)).thenReturn(success());

        // when
        sender.sendMessage(message);
        verify(successHandler, timeout(1000)).handle(eq(message), eq(subscription), any(MessageSendingResult.class));

        // then
        verifySemaphoreReleased();
        verifyRateLimiterSuccessfulSendingCountedTimes(1);
        verifyLatencyTimersCountedTimes(1, 1);
        verifyZeroInteractions(errorHandler);
        verifyZeroInteractions(failedMeter);
    }

    @Test
    public void shouldKeepTryingToSendMessageFailedSending() throws InterruptedException {
        // given
        Message message = message();
        doReturn(failure()).doReturn(failure()).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendMessage(message);
        verify(successHandler, timeout(1000)).handle(eq(message), eq(subscription), any(MessageSendingResult.class));

        // then
        verifySemaphoreReleased();
        verifyLatencyTimersCountedTimes(3, 3);
        verifyRateLimiterFailedSendingCountedTimes(2);
        verifyRateLimiterSuccessfulSendingCountedTimes(1);
        verifyErrorHandlerHandleFailed(message, subscription, 2);
    }

    @Test
    public void shouldKeepTryingToSendMessageOnRuntimeExceptionFromSender() throws InterruptedException {
        // given
        RuntimeException exception = exception();
        Message message = message();
        doThrow(exception).doThrow(exception).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendMessage(message);
        verify(successHandler, timeout(1000)).handle(eq(message), eq(subscription), any(MessageSendingResult.class));

        // then
        verifySemaphoreReleased();
        verifyLatencyTimersCountedTimes(3, 1);
        verifyRateLimiterFailedSendingCountedTimes(2);
        verifyRateLimiterSuccessfulSendingCountedTimes(1);
        verifyErrorHandlerHandleFailed(message, subscription, 2);
    }

    @Test
    public void shouldDiscardMessageWhenTTLIsExceeded() {
        // given
        RuntimeException exception = exception();
        Message message = messageWithTimestamp(System.currentTimeMillis() - 11000);
        doThrow(exception).when(messageSender).send(message);

        // when
        sender.sendMessage(message);

        // then
        verifySemaphoreReleased();
        verifyZeroInteractions(successHandler);
        verifyLatencyTimersCountedTimes(1, 0);
        verifyRateLimiterFailedSendingCountedTimes(1);
    }

    @Test
    public void shouldNotKeepTryingToSendMessageFailedWithStatusCode4xx() throws InterruptedException {
        // given
        Message message = message();
        doReturn(failure(403)).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendMessage(message);

        // then
        verify(errorHandler, timeout(1000)).handleDiscarded(eq(message), eq(subscription), any(MessageSendingResult.class));
        verifySemaphoreReleased();
        verifyZeroInteractions(successHandler);
        verifyLatencyTimersCountedTimes(1, 1);
    }

    @Test
    public void shouldKeepTryingToSendMessageFailedWithStatusCode4xxForSubscriptionWith4xxRetry() throws InterruptedException {
        // given
        ConsumerMessageSender sender = consumerMessageSender(subscriptionWith4xxRetry);
        Message message = message();
        doReturn(failure(403)).doReturn(failure(403)).doReturn(failure(403)).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendMessage(message);
        verify(successHandler, timeout(1000)).handle(eq(message), eq(subscriptionWith4xxRetry), any(MessageSendingResult.class));

        // then
        verifySemaphoreReleased();
        verify(errorHandler, timeout(1000).times(3)).handleFailed(eq(message), eq(subscriptionWith4xxRetry), any(MessageSendingResult.class));
    }

    @Test
    public void shouldTreat4xxResponseForSubscriptionWithNo4xxRetryAsSuccess() {
        // given
        Message message = message();
        doReturn(failure(403)).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendMessage(message);

        // then
        verifyRateLimiterFailedSendingCountedTimes(0);
        verifyRateLimiterSuccessfulSendingCountedTimes(1);
        verifyErrorHandlerHandleFailed(message, subscription, 1);
    }

    @Test
    public void shouldReduceSendingRateLimitOnErrorResponseOtherThan4xxForSubscriptionWithNo4xxRetry() {
        // given
        Message message = message();
        doReturn(failure(500)).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendMessage(message);

        // then
        verifyRateLimiterFailedSendingCountedTimes(1);
        verifyErrorHandlerHandleFailed(message, subscription, 1);
    }

    @Test
    public void shouldReduceSendingRateLimitOn4xxResponseForSubscriptionWith4xxRetry() {
        // given
        ConsumerMessageSender sender = consumerMessageSender(subscriptionWith4xxRetry);
        Message message = message();
        doReturn(failure(403)).doReturn(failure(403)).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendMessage(message);

        // then
        verifyRateLimiterFailedSendingCountedTimes(2);
        verifyRateLimiterSuccessfulSendingCountedTimes(1);
    }

    @Test
    public void shouldRaiseTimeoutWhenSenderNotCompletesResult() {
        // given
        Message message = message();
        when(messageSender.send(message)).thenReturn(new CompletableFuture<>());

        // when
        sender.sendMessage(message);

        // then
        verifyErrorHandlerHandleFailed(message, subscription, 1, 3000);
    }

    @Test
    public void shouldBackoffRetriesWhenEndpointFails() throws InterruptedException {
        // given
        int executionTime = 100;
        int senderBackoffTime = 50;
        Subscription subscriptionWithBackoff = subscriptionWithBackoff(senderBackoffTime);
        setUpMetrics(subscriptionWithBackoff);

        sender = consumerMessageSender(subscriptionWithBackoff);
        Message message = message();
        doReturn(failure(500)).when(messageSender).send(message);

        //when
        sender.sendMessage(message);

        //then
        Thread.sleep(executionTime);
        verifyErrorHandlerHandleFailed(message, subscriptionWithBackoff, 1 + executionTime / senderBackoffTime);
    }

    private ConsumerMessageSender consumerMessageSender(Subscription subscription) {
        return new ConsumerMessageSender(subscription, messageSender, successHandler, errorHandler, rateLimiter,
                Executors.newSingleThreadExecutor(), inflightSemaphore, hermesMetrics, ASYNC_TIMEOUT_MS,
                new FutureAsyncTimeout<>(MessageSendingResult::loggedFailResult, Executors.newSingleThreadScheduledExecutor()));
    }

    private void verifyRateLimiterSuccessfulSendingCountedTimes(int count) {
        verify(rateLimiter, timeout(1000).times(count)).registerSuccessfulSending();
    }

    private void verifyRateLimiterFailedSendingCountedTimes(int count) {
        verify(rateLimiter, timeout(1000).times(count)).registerFailedSending();
    }

    private void verifyErrorHandlerHandleFailed(Message message, Subscription subscription, int times) {
        verifyErrorHandlerHandleFailed(message, subscription, times, 1000);
    }

    private void verifyErrorHandlerHandleFailed(Message message, Subscription subscription, int times, int timeout) {
        verify(errorHandler, timeout(timeout).times(times)).handleFailed(eq(message), eq(subscription), any(MessageSendingResult.class));
    }

    private void verifyLatencyTimersCountedTimes(int timeCount, int closeCount) {
        verify(hermesMetrics, times(1)).latencyTimer(subscription);
        verify(consumerLatencyTimer, times(timeCount)).time();
        verify(consumerLatencyTimerContext, times(closeCount)).stop();
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

    private SubscriptionBuilder subscriptionBuilderWithTestValues() {
        return subscription("group.topic","subscription");
    }

    private RuntimeException exception() {
        return new RuntimeException("problem");
    }

    private CompletableFuture<MessageSendingResult> success() {
        return CompletableFuture.completedFuture(succeededResult());
    }

    private CompletableFuture<MessageSendingResult> failure() {
        return CompletableFuture.completedFuture(failedResult(exception()));
    }

    private CompletableFuture<MessageSendingResult> failure(int statusCode) {
        return CompletableFuture.completedFuture(failedResult(statusCode));
    }

    private void verifySemaphoreReleased() {
        assertThat(inflightSemaphore.availablePermits()).isEqualTo(1);
    }

    private Message message() {
        return messageWithTimestamp(System.currentTimeMillis());
    }

    private Message messageWithTimestamp(long timestamp) {

        return MessageBuilder
                .withTestMessage()
                .withContent("{\"username\":\"ala\"}", StandardCharsets.UTF_8)
                .withReadingTimestamp(timestamp)
                .build();
    }
}