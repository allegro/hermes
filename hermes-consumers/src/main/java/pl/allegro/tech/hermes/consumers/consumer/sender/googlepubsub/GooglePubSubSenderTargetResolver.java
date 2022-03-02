package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.pubsub.v1.TopicName;
import pl.allegro.tech.hermes.api.EndpointAddress;

import java.net.URI;

public class GooglePubSubSenderTargetResolver {

    public GooglePubSubSenderTarget resolve(EndpointAddress address) {
        final URI uri = URI.create(address.getRawEndpoint());
        return GooglePubSubSenderTarget.builder()
                .withPubSubEndpoint(uri.getAuthority())
                .withTopicName(TopicName.parse(uri.getPath().substring(1)))
                .build();
    }
}
