package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.auth;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;

public class ApplicationDefaultGooglePubSubCredentialsProvider implements GooglePubSubCredentialsProvider {

    private final FixedCredentialsProvider provider;

    public ApplicationDefaultGooglePubSubCredentialsProvider(FixedCredentialsProvider provider) {
        this.provider = provider;
    }

    @Override
    public CredentialsProvider getProvider() {
        return provider;
    }
}
