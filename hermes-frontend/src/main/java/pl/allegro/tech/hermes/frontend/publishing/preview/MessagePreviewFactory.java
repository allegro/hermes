package pl.allegro.tech.hermes.frontend.publishing.preview;

import org.apache.commons.lang.ArrayUtils;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreview;

import javax.inject.Inject;

public class MessagePreviewFactory {

    private final int maxMessagePreviewLength;

    @Inject
    public MessagePreviewFactory(ConfigFactory configFactory) {
        this(configFactory.getIntProperty(Configs.FRONTEND_MESSAGE_PREVIEW_MAX_SIZE_KB) * 1024);
    }

    MessagePreviewFactory(int maxMessagePreviewSize) {
        this.maxMessagePreviewLength = maxMessagePreviewSize;
    }

    public MessagePreview create(byte[] content) {
        final boolean truncated = (content.length > maxMessagePreviewLength);

        if (truncated) {
            return new MessagePreview(ArrayUtils.subarray(content, 0, maxMessagePreviewLength), true);
        } else {
            return new MessagePreview(content);
        }
    }
}
