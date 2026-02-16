package pl.allegro.tech.hermes.frontend.listeners;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

/**
 * @deprecated This feature is deprecated and will be removed in a future version.
 */
@Deprecated
public interface BrokerTimeoutListener {

  void onTimeout(Message message, Topic topic);
}
