package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

public record PublishingChaosPolicy(boolean enabled, Map<String, DatacenterChaosPolicy> datacenterChaosPolicies) {

    @JsonCreator
    public PublishingChaosPolicy(@JsonProperty("enabled") boolean enabled,
                                 @JsonProperty("datacenterChaosPolicies") Map<String, DatacenterChaosPolicy> datacenterChaosPolicies) {
        this.enabled = enabled;
        this.datacenterChaosPolicies = datacenterChaosPolicies;
    }

    public static PublishingChaosPolicy disabled() {
        return new PublishingChaosPolicy(false, Collections.emptyMap());
    }

    public record DatacenterChaosPolicy(int delayFrom, int delayTo, boolean completeWithError) {

        @JsonCreator
        public DatacenterChaosPolicy(@JsonProperty("delayFrom") int delayFrom,
                                     @JsonProperty("delayTo") int delayTo,
                                     @JsonProperty("completeWithError") boolean completeWithError) {
            this.delayFrom = delayFrom;
            this.delayTo = delayTo;
            this.completeWithError = completeWithError;
        }
    }
}
