package pl.allegro.tech.hermes.frontend.buffer;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.listeners.BrokerAcknowledgeListener;
import pl.allegro.tech.hermes.frontend.listeners.BrokerErrorListener;
import pl.allegro.tech.hermes.frontend.listeners.BrokerTimeoutListener;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

public class BrokerListener
    implements BrokerAcknowledgeListener, BrokerTimeoutListener, BrokerErrorListener {

  private final MessageRepository messageRepository;

  public BrokerListener(MessageRepository messageRepository) {
    this.messageRepository = messageRepository;
  }

  @Override
  public void onAcknowledge(Message message, Topic topic) {
    messageRepository.delete(message.getId());
  }

  @Override
  public void onTimeout(Message message, Topic topic) {
    messageRepository.save(message, topic);
  }

  @Override
  public void onError(Message message, Topic topic, Exception ex) {
    messageRepository.save(message, topic);
  }
}
