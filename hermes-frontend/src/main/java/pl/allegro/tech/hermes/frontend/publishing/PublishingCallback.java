package pl.allegro.tech.hermes.frontend.publishing;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

public interface PublishingCallback {

  /** Invoked when publishing to the broker fails and the message won't be delivered. */
  void onUnpublished(Message message, Topic topic, Exception exception);

  /** Invoked the first time the message is successfully published to the broker. */
  void onPublished(Message message, Topic topic);

  /**
   * Invoked every time the message is successfully published to the broker. Could be invoked one or
   * many times depending on the underlying implementation.
   *
   * @param message the delivered message
   * @param topic the topic that the message was delivered to
   * @param datacenter the datacenter that the messages was delivered to
   */
  void onEachPublished(Message message, Topic topic, String datacenter);
}
