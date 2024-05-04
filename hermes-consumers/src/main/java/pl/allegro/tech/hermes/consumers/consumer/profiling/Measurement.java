package pl.allegro.tech.hermes.consumers.consumer.profiling;

public enum Measurement {
    SIGNALS_AND_SEMAPHORE_ACQUIRE("signalsAndSemaphoreAcquire"),
    SIGNALS_INTERRUPT_RUN("signalsInterrupt.run"),
    SCHEDULE_RESEND("schedule.resend"),
    MESSAGE_RECEIVER_NEXT("messageReceiver.next"),
    MESSAGE_CONVERSION("messageConverter.convert"),
    OFFER_INFLIGHT_OFFSET("offsetQueue.offerInflightOffset"),
    TRACKERS_LOG_INFLIGHT("trackers.logInflight"),
    ACQUIRE_RATE_LIMITER("acquireRateLimiter"),
    MESSAGE_SENDER_SEND("messageSender.send"),
    HANDLERS("handlers");

    private final String description;

    Measurement(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
