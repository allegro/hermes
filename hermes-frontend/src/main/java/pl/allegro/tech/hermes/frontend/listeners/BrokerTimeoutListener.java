package pl.allegro.tech.hermes.frontend.listeners;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

public interface BrokerTimeoutListener {

  void onTimeout(Message message, Topic topic);
}
