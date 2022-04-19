package pl.allegro.tech.hermes.frontend.publishing.preview;

import org.apache.commons.lang.ArrayUtils;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreview;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageToJsonConverter;

import javax.inject.Inject;

public class MessagePreviewFactory {

    private final int maxMessagePreviewLength;
    private final MessageToJsonConverter converter;

    @Inject
    public MessagePreviewFactory(ConfigFactory configFactory) {
        this(configFactory.getIntProperty(Configs.FRONTEND_MESSAGE_PREVIEW_MAX_SIZE_KB) * 1024);
    }

    MessagePreviewFactory(int maxMessagePreviewSize) {
        this.maxMessagePreviewLength = maxMessagePreviewSize;
        converter = new MessageToJsonConverter();
    }

    public MessagePreview create(Message message, boolean schemaIdAwareSerializationEnabled) {
        byte[] content = converter.convert(message, schemaIdAwareSerializationEnabled);
        final boolean truncated = (content.length > maxMessagePreviewLength);

        if (truncated) {
            return new MessagePreview(ArrayUtils.subarray(content, 0, maxMessagePreviewLength), true);
        } else {
            return new MessagePreview(content);
        }
    }
}
