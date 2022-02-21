package pl.allegro.tech.hermes.consumers.consumer.sender.pubsub;

import com.google.api.gax.core.CredentialsProvider;

import java.io.IOException;

public interface PubSubCredentialsProvider {

    CredentialsProvider getProvider() throws IOException;

}
