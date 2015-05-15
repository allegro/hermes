package pl.allegro.tech.hermes.consumers.consumer.receiver;

import pl.allegro.tech.hermes.consumers.consumer.message.RawMessage;

import java.util.List;

public class SplitMessagesReceiver {

    private MessageReceiver messageReceiver;
    private MessageSplitter messageSplitter;

    public SplitMessagesReceiver(MessageReceiver messageReceiver, MessageSplitter messageSplitter) {
        this.messageSplitter = messageSplitter;
        this.messageReceiver = messageReceiver;
    }

    public List<Message> next() {
        return split(messageReceiver.next());
    }

    private List<Message> split(RawMessage rawMessage) {
        return messageSplitter.extractMessages(rawMessage);
    }

    public void stop() {
        messageReceiver.stop();
    }

}
