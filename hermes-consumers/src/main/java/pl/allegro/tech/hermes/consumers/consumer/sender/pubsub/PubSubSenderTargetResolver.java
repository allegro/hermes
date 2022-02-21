package pl.allegro.tech.hermes.consumers.consumer.sender.pubsub;

import com.google.pubsub.v1.TopicName;
import pl.allegro.tech.hermes.api.EndpointAddress;

import java.net.URI;

public class PubSubSenderTargetResolver {

    public PubSubSenderTarget resolve(EndpointAddress address) {
        final URI uri = URI.create(address.getRawEndpoint());
        return PubSubSenderTarget.builder()
                .withPubSubEndpoint(uri.getAuthority())
                .withTopicName(TopicName.parse(uri.getPath().substring(1)))
                .build();
    }
}
