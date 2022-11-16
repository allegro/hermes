package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class GooglePubSubMessageTransformerProvider {

    private static final Logger logger = LoggerFactory.getLogger(GooglePubSubMessageTransformerProvider.class);

    private final GooglePubSubMessageTransformerCreator messageTransformerCreator;

    public GooglePubSubMessageTransformerProvider(GooglePubSubMessageTransformerCreator messageTransformerCreator) {
        this.messageTransformerCreator = messageTransformerCreator;
    }

    GooglePubSubMessageTransformer getTransformerForTargetEndpoint(GooglePubSubSenderTarget pubSubTarget) {
        if (pubSubTarget.isCompressionRequested()) {
            Optional<GooglePubSubMessageTransformer> compressingTransformer =
                    messageTransformerCreator.transformerForCodec(pubSubTarget.getCompressionCodec());
            if (compressingTransformer.isPresent()) {
                return compressingTransformer.get();
            } else {
                logger.warn("PubSub message compression is disabled, switching to raw transfer for {}.", pubSubTarget.getTopicName());
            }
        }
        return messageTransformerCreator.rawTransformer();
    }
}
