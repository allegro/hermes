package pl.allegro.tech.hermes.management.domain.subscription.validator;

import java.util.List;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.Subscription;

public class SubscriberWithAccessToAnyTopic {
  private final OwnerId ownerId;
  private final List<String> protocols;

  public SubscriberWithAccessToAnyTopic(
      String ownerSource, String ownerId, List<String> protocols) {
    this.ownerId = new OwnerId(ownerSource, ownerId);
    this.protocols = protocols;
  }

  boolean matches(Subscription subscription) {
    return ownerId.equals(subscription.getOwner())
        && protocols.contains(subscription.getEndpoint().getProtocol());
  }
}
