package pl.allegro.tech.hermes.management.domain.subscription.consumergroup;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import pl.allegro.tech.hermes.api.SubscriptionName;

public record ConsumerGroupToDelete(
    @JsonProperty("subscriptionName") SubscriptionName subscriptionName,
    @JsonProperty("datacenter") String datacenter,
    @JsonProperty("requestedAt") Instant requestedAt) {}
