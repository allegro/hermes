package pl.allegro.tech.hermes.common.admin;

import pl.allegro.tech.hermes.api.SubscriptionName;

public interface AdminTool {

  void retransmit(SubscriptionName subscriptionName);

  enum Operations {
    RETRANSMIT
  }
}
