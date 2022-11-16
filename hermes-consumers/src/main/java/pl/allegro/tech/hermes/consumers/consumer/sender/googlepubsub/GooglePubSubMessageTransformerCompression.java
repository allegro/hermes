package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

import java.io.IOException;

class GooglePubSubMessageTransformerCompression implements GooglePubSubMessageTransformer {

    private final MetadataAppender<PubsubMessage> metadataAppender;
    private final MessageCompressor compressor;

    GooglePubSubMessageTransformerCompression(GooglePubSubMetadataCompressionAppender metadataAppender,
                                              MessageCompressor compressor) {
        this.metadataAppender = metadataAppender;
        this.compressor = compressor;
    }

    @Override
    public PubsubMessage fromHermesMessage(Message message) {
        try {
            final PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFrom(
                            compressor.compress(message.getData())
                    )).build();

            return metadataAppender.append(pubsubMessage, message);
        } catch (IOException e) {
            throw new GooglePubSubMessageCompressionException("Error on PubSub message compression", e);
        }
    }
}
