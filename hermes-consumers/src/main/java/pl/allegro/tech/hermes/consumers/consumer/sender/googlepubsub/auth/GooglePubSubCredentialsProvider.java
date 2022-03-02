package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.auth;

import com.google.api.gax.core.CredentialsProvider;

import java.io.IOException;

public interface GooglePubSubCredentialsProvider {

    CredentialsProvider getProvider() throws IOException;

}
