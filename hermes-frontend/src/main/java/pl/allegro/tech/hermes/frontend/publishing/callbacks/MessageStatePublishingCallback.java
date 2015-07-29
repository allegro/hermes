package pl.allegro.tech.hermes.frontend.publishing.callbacks;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;

import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.SENT_TO_KAFKA;

public class MessageStatePublishingCallback implements PublishingCallback {

    private final MessageState messageState;

    public MessageStatePublishingCallback(MessageState messageState) {
        this.messageState = messageState;
    }

    @Override
    public void onUnpublished(Exception exception) {
    }

    @Override
    public void onPublished(Message message, Topic topic) {
        messageState.setState(SENT_TO_KAFKA);
    }
}
