package pl.allegro.tech.hermes.consumers.supervisor.process;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class RunningSubscriptionStatus {
  private final String qualifiedName;
  private final Map<Signal.SignalType, Long> signalTimesheet;

  public RunningSubscriptionStatus(
      @JsonProperty("qualifiedName") String qualifiedName,
      @JsonProperty("signals") Map<Signal.SignalType, Long> signalTimesheet) {

    this.qualifiedName = qualifiedName;
    this.signalTimesheet = signalTimesheet;
  }

  public String getQualifiedName() {
    return qualifiedName;
  }

  public Map<Signal.SignalType, Long> getSignalTimesheet() {
    return signalTimesheet;
  }
}
