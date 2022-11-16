package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.util.Map;

class GooglePubSubMetadataCompressionAppender extends GooglePubSubMetadataAppender {

    public static final String HEADER_NAME_CODEC_NAME = "cn";
    private final String codecHeader;

    GooglePubSubMetadataCompressionAppender(CompressionCodec codec) {
        this.codecHeader = codec.getHeader();
    }

    @Override
    protected Map<String, String> createMessageAttributes(Message message) {
        Map<String, String> messageAttributes = super.createMessageAttributes(message);
        messageAttributes.put(HEADER_NAME_CODEC_NAME, codecHeader);
        return messageAttributes;
    }
}
