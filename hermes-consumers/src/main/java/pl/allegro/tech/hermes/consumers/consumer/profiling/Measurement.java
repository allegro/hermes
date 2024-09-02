package pl.allegro.tech.hermes.consumers.consumer.profiling;

public class Measurement {
  public static final String SIGNALS_AND_SEMAPHORE_ACQUIRE = "signalsAndSemaphoreAcquire";
  public static final String SIGNALS_INTERRUPT_RUN = "signalsInterrupt.run";
  public static final String SCHEDULE_RESEND = "schedule.resend";
  public static final String MESSAGE_RECEIVER_NEXT = "messageReceiver.next";
  public static final String MESSAGE_CONVERSION = "messageConverter.convert";
  public static final String OFFER_INFLIGHT_OFFSET = "offsetQueue.offerInflightOffset";
  public static final String TRACKERS_LOG_INFLIGHT = "trackers.logInflight";
  public static final String SCHEDULE_MESSAGE_SENDING = "retrySingleThreadExecutor.schedule";
  public static final String ACQUIRE_RATE_LIMITER = "acquireRateLimiter";
  public static final String MESSAGE_SENDER_SEND = "messageSender.send";
  public static final String HANDLERS = "handlers";
}
