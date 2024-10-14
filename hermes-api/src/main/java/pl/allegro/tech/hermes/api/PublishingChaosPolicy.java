package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.Map;

public record PublishingChaosPolicy(
    ChaosMode mode, ChaosPolicy globalPolicy, Map<String, ChaosPolicy> datacenterPolicies) {

  @JsonCreator
  public PublishingChaosPolicy(
      @JsonProperty("mode") ChaosMode mode,
      @JsonProperty("globalPolicy") ChaosPolicy globalPolicy,
      @JsonProperty("datacenterPolicies") Map<String, ChaosPolicy> datacenterPolicies) {
    this.mode = mode;
    this.globalPolicy = globalPolicy;
    this.datacenterPolicies = datacenterPolicies == null ? Map.of() : datacenterPolicies;
  }

  public static PublishingChaosPolicy disabled() {
    return new PublishingChaosPolicy(ChaosMode.DISABLED, null, Collections.emptyMap());
  }

  public record ChaosPolicy(
      int probability, int delayFrom, int delayTo, boolean completeWithError) {

    @JsonCreator
    public ChaosPolicy(
        @JsonProperty("probability") int probability,
        @JsonProperty("delayFrom") int delayFrom,
        @JsonProperty("delayTo") int delayTo,
        @JsonProperty("completeWithError") boolean completeWithError) {
      this.probability = probability;
      this.delayFrom = delayFrom;
      this.delayTo = delayTo;
      this.completeWithError = completeWithError;
    }
  }

  public enum ChaosMode {
    DISABLED,
    GLOBAL,
    DATACENTER
  }
}
