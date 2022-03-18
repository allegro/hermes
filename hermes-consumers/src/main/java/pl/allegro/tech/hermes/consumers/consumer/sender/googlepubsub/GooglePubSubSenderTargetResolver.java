package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.common.base.Preconditions;
import com.google.pubsub.v1.TopicName;
import pl.allegro.tech.hermes.api.EndpointAddress;

import java.net.URI;

public class GooglePubSubSenderTargetResolver {

    public GooglePubSubSenderTarget resolve(EndpointAddress address) {
        try {
            final URI uri = URI.create(address.getRawEndpoint());
            Preconditions.checkArgument(uri.getScheme().equals("googlepubsub"));
            Preconditions.checkArgument(uri.getPort() > 0);

            return GooglePubSubSenderTarget.builder()
                    .withPubSubEndpoint(uri.getAuthority())
                    .withTopicName(TopicName.parse(uri.getPath().substring(1)))
                    .build();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Given endpoint address is invalid", e);
        }
    }
}
