package pl.allegro.tech.hermes.frontend.publishing.metadata;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import javax.inject.Inject;
import java.util.Map;

public class MetadataAddingMessageConverter {

    private final MessageContentWrapper messageContentWrapper;

    @Inject
    public MetadataAddingMessageConverter(MessageContentWrapper messageContentWrapper) {
        this.messageContentWrapper = messageContentWrapper;
    }

    public Message addMetadata(Message toEnrich, Topic topic, Map<String, String> externalMetadata) {
        byte[] wrappedData = messageContentWrapper.wrap(toEnrich.getData(), toEnrich.getId(),
                toEnrich.getTimestamp(), topic, externalMetadata);
        return toEnrich.withDataReplaced(wrappedData);
    }

}
