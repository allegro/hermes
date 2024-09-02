package pl.allegro.tech.hermes.frontend.listeners;

import java.util.ArrayList;
import java.util.List;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

public class BrokerListeners {

  private final List<BrokerTimeoutListener> timeoutListeners = new ArrayList<>();
  private final List<BrokerAcknowledgeListener> acknowledgeListeners = new ArrayList<>();
  private final List<BrokerErrorListener> errorListeners = new ArrayList<>();

  public void addTimeoutListener(BrokerTimeoutListener brokerTimeoutListener) {
    timeoutListeners.add(brokerTimeoutListener);
  }

  public void addAcknowledgeListener(BrokerAcknowledgeListener brokerAcknowledgeListener) {
    acknowledgeListeners.add(brokerAcknowledgeListener);
  }

  public void addErrorListener(BrokerErrorListener brokerErrorListener) {
    errorListeners.add(brokerErrorListener);
  }

  public void onAcknowledge(Message message, Topic topic) {
    acknowledgeListeners.forEach(l -> l.onAcknowledge(message, topic));
  }

  public void onTimeout(Message message, Topic topic) {
    timeoutListeners.forEach(l -> l.onTimeout(message, topic));
  }

  public void onError(Message message, Topic topic, Exception ex) {
    errorListeners.forEach(l -> l.onError(message, topic, ex));
  }
}
