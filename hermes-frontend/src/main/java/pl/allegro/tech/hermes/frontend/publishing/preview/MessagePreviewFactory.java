package pl.allegro.tech.hermes.frontend.publishing.preview;

import org.apache.commons.lang3.ArrayUtils;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreview;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageToJsonConverter;

public class MessagePreviewFactory {

    private final int maxMessagePreviewLength;
    private final MessageToJsonConverter converter;

    public MessagePreviewFactory(int maxMessagePreviewSizeKb) {
        this.maxMessagePreviewLength = maxMessagePreviewSizeKb * 1024;
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
