package pl.allegro.tech.hermes.consumers.consumer.sender;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MessageSenderProviders {
    private final Map<String, List<ProtocolMessageSenderProvider>> entries = new HashMap<>();

    public MessageSenderProviders(ProtocolMessageSenderProvider defaultHttpMessageSenderProvider,
                                  ProtocolMessageSenderProvider defaultHttpsMessageSenderProvider,
                                  ProtocolMessageSenderProvider defaultJmsMessageSenderProvider) {
        addEntry("http", defaultHttpMessageSenderProvider);
        addEntry("https", defaultHttpsMessageSenderProvider);
        addEntry("jms", defaultJmsMessageSenderProvider);
    }

    public void populateMessageSenderFactory(MessageSenderFactory messageSenderFactory) {
        entries.forEach((key, value) -> value.forEach(messageSender ->
                messageSenderFactory.addSupportedProtocol(key, messageSender)
        ));
    }

    public void addEntry(String key, ProtocolMessageSenderProvider object) {
        addEntries(key, Collections.singletonList(object));
    }

    public void addEntries(String key, List<ProtocolMessageSenderProvider> objects) {
        List<ProtocolMessageSenderProvider> currentList = entries.computeIfAbsent(key, k -> new LinkedList<>());
        currentList.addAll(objects);
    }
}
