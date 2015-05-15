package pl.allegro.tech.hermes.consumers.consumer;

import com.codahale.metrics.Meter;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.LatencyTimer;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;
import pl.allegro.tech.hermes.consumers.consumer.result.ErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.SuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.common.metric.Metrics.Meter.CONSUMER_FAILED_METER;
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult;
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.succeededResult;

@RunWith(MockitoJUnitRunner.class)
public class ConsumerMessageSenderTest {

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
    private LatencyTimer latencyTimer;

    @Mock
    private Meter failedMeter;

    private Semaphore inflightSemaphore;

    private ConsumerMessageSender sender;

    @Before
    public void setUp() {
        setUpMetrics(subscription);
        setUpMetrics(subscriptionWith4xxRetry);
        inflightSemaphore = new Semaphore(0);
    }

    private void setUpMetrics(Subscription subscription) {
        when(hermesMetrics.latencyTimer(subscription)).thenReturn(latencyTimer);
        when(hermesMetrics.meter(CONSUMER_FAILED_METER, subscription.getTopicName(), subscription.getName())).thenReturn(failedMeter);
    }

    @Test
    public void shouldHandleSuccessfulSending() {
        // given
        sender = consumerMessageSender(subscription);
        Message message = message();
        when(messageSender.send(message)).thenReturn(success());

        // when
        sender.sendMessage(message);
        verify(successHandler, timeout(1000)).handle(message, subscription);

        // then
        verifySemaphoreReleased();
        verifyRateLimiterSuccessfulSendingCountedTimes(1);
        verifyLatencyTimersCountedTimes(1);
        verifyZeroInteractions(errorHandler);
        verifyZeroInteractions(failedMeter);
    }

    @Test
    public void shouldKeepTryingToSendMessageFailedSending() throws InterruptedException {
        // given
        sender = consumerMessageSender(subscription);
        Message message = message();
        doReturn(failure()).doReturn(failure()).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendMessage(message);
        verify(successHandler, timeout(1000)).handle(message, subscription);

        // then
        verifySemaphoreReleased();
        verifyLatencyTimersCountedTimes(3);
        verifyRateLimiterFailedSendingCountedTimes(2);
        verifyRateLimiterSuccessfulSendingCountedTimes(1);
        verifyErrorHandlerHandleFailed(message, subscription, 2);
    }

    @Test
    public void shouldKeepTryingToSendMessageOnRuntimeExceptionFromSender() throws InterruptedException {
        // given
        sender = consumerMessageSender(subscription);
        RuntimeException exception = exception();
        Message message = message();
        doThrow(exception).doThrow(exception).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendMessage(message);
        verify(successHandler, timeout(1000)).handle(message, subscription);

        // then
        verifySemaphoreReleased();
        verifyLatencyTimersCountedTimes(3);
        verifyRateLimiterFailedSendingCountedTimes(2);
        verifyRateLimiterSuccessfulSendingCountedTimes(1);
        verifyErrorHandlerHandleFailed(message, subscription, 2);
    }

    @Test
    public void shouldDiscardMessageWhenTTLIsExceeded() {
        // given
        sender = consumerMessageSender(subscription);
        RuntimeException exception = exception();
        Message message = messageWithTimestamp(System.currentTimeMillis() - 11000);
        doThrow(exception).when(messageSender).send(message);

        // when
        sender.sendMessage(message);

        // then
        verifySemaphoreReleased();
        verifyZeroInteractions(successHandler);
        verifyLatencyTimersCountedTimes(1);
        verifyRateLimiterFailedSendingCountedTimes(1);
    }

    @Test
    public void shouldNotKeepTryingToSendMessageFailedWithStatusCode4xx() throws InterruptedException {
        // given
        sender = consumerMessageSender(subscription);
        Message message = message();
        doReturn(failure(403)).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendMessage(message);

        // then
        verify(errorHandler, timeout(1000)).handleDiscarded(eq(message), eq(subscription), any(MessageSendingResult.class));
        verifySemaphoreReleased();
        verifyZeroInteractions(successHandler);
        verifyLatencyTimersCountedTimes(1);
    }

    @Test
    public void shouldKeepTryingToSendMessageFailedWithStatusCode4xxForSubscriptionWith4xxRetry() throws InterruptedException {
        // given
        sender = consumerMessageSender(subscriptionWith4xxRetry);
        Message message = message();
        doReturn(failure(403)).doReturn(failure(403)).doReturn(failure(403)).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendMessage(message);
        verify(successHandler, timeout(1000)).handle(message, subscriptionWith4xxRetry);

        // then
        verifySemaphoreReleased();
        verify(errorHandler, timeout(1000).times(3)).handleFailed(eq(message), eq(subscriptionWith4xxRetry), any(MessageSendingResult.class));
    }

    @Test
    public void shouldNotReduceSendingRateLimitOn4xxResponseForSubscriptionWithNo4xxRetry() {
        // given
        sender = consumerMessageSender(subscription);
        Message message = message();
        doReturn(failure(403)).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendMessage(message);

        // then
        verifyRateLimiterFailedSendingCountedTimes(0);
        verifyErrorHandlerHandleFailed(message, subscription, 1);
    }

    @Test
    public void shouldReduceSendingRateLimitOnErrorResponseOtherThan4xxForSubscriptionWithNo4xxRetry() {
        // given
        sender = consumerMessageSender(subscription);
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
        sender = consumerMessageSender(subscriptionWith4xxRetry);
        Message message = message();
        doReturn(failure(403)).doReturn(failure(403)).doReturn(success()).when(messageSender).send(message);

        // when
        sender.sendMessage(message);

        // then
        verifyRateLimiterFailedSendingCountedTimes(2);
        verifyRateLimiterSuccessfulSendingCountedTimes(1);
    }

    private ConsumerMessageSender consumerMessageSender(Subscription subscription) {
        return new ConsumerMessageSender(subscription, messageSender, successHandler, errorHandler, rateLimiter,
                Executors.newSingleThreadExecutor(), inflightSemaphore, hermesMetrics);
    }

    private void verifyRateLimiterSuccessfulSendingCountedTimes(int count) {
        verify(rateLimiter, timeout(1000).times(count)).registerSuccessfulSending();
    }

    private void verifyRateLimiterFailedSendingCountedTimes(int count) {
        verify(rateLimiter, timeout(1000).times(count)).registerFailedSending();
    }

    private void verifyErrorHandlerHandleFailed(Message message, Subscription subscription, int times) {
        verify(errorHandler, timeout(1000).times(times)).handleFailed(eq(message), eq(subscription), any(MessageSendingResult.class));
    }

    private void verifyLatencyTimersCountedTimes(int count) {
        verify(hermesMetrics, times(count)).latencyTimer(subscription);
        verify(latencyTimer, times(count)).stop();
    }

    private Subscription subscriptionWithTtl(int ttl) {
        return subscription().withName("subscription").withSubscriptionPolicy(subscriptionPolicy().withMessageTtl(ttl).build()).build();
    }

    private Subscription subscriptionWithTtlAndClientErrorRetry(int ttl) {
        return subscription().withName("subscription2").withSubscriptionPolicy(subscriptionPolicy().applyDefaults().withMessageTtl(ttl).withClientErrorRetry().build()).build();
    }

    private RuntimeException exception() {
        return new RuntimeException("problem");
    }

    private SettableFuture<MessageSendingResult> success() {
        SettableFuture<MessageSendingResult> toBeReturned = SettableFuture.create();
        toBeReturned.set(succeededResult());
        return toBeReturned;
    }

    private SettableFuture<MessageSendingResult> failure() {
        SettableFuture<MessageSendingResult> toBeReturned = SettableFuture.create();
        toBeReturned.set(failedResult(exception()));
        return toBeReturned;
    }

    private SettableFuture<MessageSendingResult> failure(int statusCode) {
        SettableFuture<MessageSendingResult> toBeReturned = SettableFuture.create();
        toBeReturned.set(failedResult(statusCode));
        return toBeReturned;
    }

    private void verifySemaphoreReleased() {
        assertThat(inflightSemaphore.availablePermits()).isEqualTo(1);
    }

    private Message message() {
        return messageWithTimestamp(System.currentTimeMillis());
    }

    private Message messageWithTimestamp(long timestamp) {
        return new Message(Optional.of("id"), 10, 0, "topic", "{\"username\":\"ala\"}".getBytes(), Optional.of(122424L), Optional.of(timestamp));
    }
}