package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.rate.SerialConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;

import java.util.List;
import java.util.function.Predicate;

public class SendFutureProviderSupplier {
    private final ConsumerRateLimiter rateLimiter;
    private final FutureAsyncTimeout futureAsyncTimeout;
    private final int requestTimeout;
    private final List<Predicate<MessageSendingResult>> ignore;
    private final int asyncTimeoutMs;

    public SendFutureProviderSupplier(ConsumerRateLimiter rateLimiter,
                                      FutureAsyncTimeout futureAsyncTimeout,
                                      int requestTimeout,
                                      int asyncTimeoutMs,
                                      List<Predicate<MessageSendingResult>> ignore) {
        this.rateLimiter = rateLimiter;
        this.futureAsyncTimeout = futureAsyncTimeout;
        this.requestTimeout = requestTimeout;
        this.ignore = ignore;
        this.asyncTimeoutMs = asyncTimeoutMs;
    }

    public <T extends MessageSendingResult> ThrottlingSendFutureProvider<T> supply() {
        return new ThrottlingSendFutureProvider<T>(
                rateLimiter,
                ignore,
                futureAsyncTimeout,
                requestTimeout,
                asyncTimeoutMs
        );
    }

    public SendFutureProviderSupplier mutate(Integer requestTimeout, List<Predicate<MessageSendingResult>> ignore) {
        return new SendFutureProviderSupplier(
                this.rateLimiter, this.futureAsyncTimeout, requestTimeout, this.asyncTimeoutMs, ignore);
    }
}
