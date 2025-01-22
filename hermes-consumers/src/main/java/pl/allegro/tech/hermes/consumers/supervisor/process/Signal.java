package pl.allegro.tech.hermes.consumers.supervisor.process;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import pl.allegro.tech.hermes.api.SubscriptionName;

public class Signal {

  private final SignalType type;

  private final SubscriptionName target;

  private final Object payload;

  private final long executeAfterTimestamp;
  private final long id;

  public enum SignalType {
    START,
    STOP,
    RETRANSMIT,
    UPDATE_SUBSCRIPTION,
    UPDATE_TOPIC
  }

  private static final AtomicLong SIGNALS_COUNTER = new AtomicLong();

  private Signal(
      SignalType type,
      SubscriptionName target,
      Object payload,
      long executeAfterTimestamp,
      long id) {
    this.type = type;
    this.target = target;
    this.payload = payload;
    this.executeAfterTimestamp = executeAfterTimestamp;
    this.id = id;
  }

  public static Signal of(SignalType type, SubscriptionName target) {
    return of(type, target, null);
  }

  public static Signal of(SignalType type, SubscriptionName target, Object payload) {
    return of(type, target, payload, -1);
  }

  public static Signal of(
      SignalType type, SubscriptionName target, Object payload, long executeAfterTimestamp) {
    return new Signal(
        type, target, payload, executeAfterTimestamp, SIGNALS_COUNTER.incrementAndGet());
  }

  Signal createChild(SignalType type) {
    return new Signal(type, target, payload, executeAfterTimestamp, id);
  }

  SignalType getType() {
    return type;
  }

  SubscriptionName getTarget() {
    return target;
  }

  boolean canExecuteNow(long currentTimestamp) {
    return currentTimestamp >= executeAfterTimestamp;
  }

  @SuppressWarnings("unchecked")
  <T> T getPayload() {
    return (T) payload;
  }

  @Override
  public String toString() {
    return "Signal(" + id + ", " + type + ", " + target + ")";
  }

  public String getLogWithIdAndType() {
    return "[Signal(" + id + ", " + type + ")]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Signal signal)) {
      return false;
    }
    return type == signal.type && Objects.equals(target, signal.target);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, target);
  }
}
