package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SubscriptionConstraints {

    private final SubscriptionName subscriptionName;
    private final Constraints constraints;

    @JsonCreator
    public SubscriptionConstraints(@JsonProperty("subscriptionName") String subscriptionName,
                                   @JsonProperty("constraints") Constraints constraints) {
        this.subscriptionName = SubscriptionName.fromString(subscriptionName);
        this.constraints = constraints;
    }

    public SubscriptionName getSubscriptionName() {
        return subscriptionName;
    }

    public Constraints getConstraints() {
        return constraints;
    }
}
