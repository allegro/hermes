package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

public class SubscriptionConstraints {

  private final SubscriptionName subscriptionName;
  @Valid private final Constraints constraints;

  @JsonCreator
  public SubscriptionConstraints(
      @JsonProperty("subscriptionName") String subscriptionName,
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
