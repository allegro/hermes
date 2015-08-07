package pl.allegro.tech.hermes.frontend.publishing.metadata;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapperDispatcher;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import javax.inject.Inject;

public class MetadataAddingMessageConverter {

    private final MessageContentWrapperDispatcher messageContentWrapperDispatcher;

    @Inject
    public MetadataAddingMessageConverter(MessageContentWrapperDispatcher messageContentWrapperDispatcher) {
        this.messageContentWrapperDispatcher = messageContentWrapperDispatcher;
    }

    public Message addMetadata(Message toEnrich, Topic topic) {
        byte[] wrappedData = messageContentWrapperDispatcher.wrap(toEnrich.getData(), toEnrich.getId(), toEnrich.getTimestamp(), topic);
        return toEnrich.withDataReplaced(wrappedData);
    }

}
