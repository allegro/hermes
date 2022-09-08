package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import pl.allegro.tech.hermes.consumers.consumer.Message;
import java.util.Map;

public class GooglePubSubMetadataCompressionAppender extends GooglePubSubMetadataAppender {

    public static final String HEADER_NAME_CODEC_NAME = "cn";
    private final String codecName;

    public GooglePubSubMetadataCompressionAppender(String codecName) {
        this.codecName = codecName;
    }

    @Override
    protected Map<String, String> createMessageAttributes(Message message) {
        Map<String, String> messageAttributes = super.createMessageAttributes(message);
        messageAttributes.put(HEADER_NAME_CODEC_NAME, codecName);
        return messageAttributes;
    }
}
