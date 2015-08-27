package pl.allegro.tech.hermes.frontend.publishing.metadata;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import javax.inject.Inject;

public class MetadataAddingMessageConverter {

    private final MessageContentWrapper messageContentWrapper;

    @Inject
    public MetadataAddingMessageConverter(MessageContentWrapper messageContentWrapper) {
        this.messageContentWrapper = messageContentWrapper;
    }

    public Message addMetadata(Message toEnrich, Topic topic) {
        byte[] wrappedData = messageContentWrapper.wrap(toEnrich.getData(), toEnrich.getId(), toEnrich.getTimestamp(), topic);
        return toEnrich.withDataReplaced(wrappedData);
    }

}
