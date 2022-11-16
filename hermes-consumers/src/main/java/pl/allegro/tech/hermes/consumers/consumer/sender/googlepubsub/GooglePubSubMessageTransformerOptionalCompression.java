package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.pubsub.v1.PubsubMessage;
import pl.allegro.tech.hermes.consumers.consumer.Message;

class GooglePubSubMessageTransformerOptionalCompression implements GooglePubSubMessageTransformer {

    private final Long compressionThresholdBytes;
    private final GooglePubSubMessageTransformerRaw rawTransformer;
    private final GooglePubSubMessageTransformerCompression compressionTransformer;

    GooglePubSubMessageTransformerOptionalCompression(Long compressionThresholdBytes,
                                                      GooglePubSubMessageTransformerRaw rawTransformer,
                                                      GooglePubSubMessageTransformerCompression compressionTransformer) {
        this.compressionThresholdBytes = compressionThresholdBytes;
        this.rawTransformer = rawTransformer;
        this.compressionTransformer = compressionTransformer;
    }

    @Override
    public PubsubMessage fromHermesMessage(Message message) {
        byte[] data = message.getData();
        if (data.length > compressionThresholdBytes) {
            return compressionTransformer.fromHermesMessage(message);
        } else {
            return rawTransformer.fromHermesMessage(message);
        }
    }
}
