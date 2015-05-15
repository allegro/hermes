package pl.allegro.tech.hermes.frontend.listeners;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.Message;

import java.util.ArrayList;
import java.util.List;

public class BrokerListeners {

    private final List<BrokerTimeoutListener> timeoutListeners = new ArrayList<>();

    private final List<BrokerAcknowledgeListener> acknowledgeListeners = new ArrayList<>();

    public void addTimeoutListener(BrokerTimeoutListener brokerTimeoutListener) {
        timeoutListeners.add(brokerTimeoutListener);
    }

    public void addAcknowledgeListener(BrokerAcknowledgeListener brokerAcknowledgeListener) {
        acknowledgeListeners.add(brokerAcknowledgeListener);
    }

    public void onAcknowledge(Message message, Topic topic) {
        acknowledgeListeners.forEach(l -> l.onAcknowledge(message, topic));
    }

    public void onTimeout(Message message, Topic topic) {
        timeoutListeners.forEach(l -> l.onTimeout(message, topic));
    }
}
