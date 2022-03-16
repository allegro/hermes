package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.auth;

import com.google.api.gax.core.CredentialsProvider;

public class ApplicationDefaultGooglePubSubCredentialsProvider implements GooglePubSubCredentialsProvider {

    private final CredentialsProvider provider;

    public ApplicationDefaultGooglePubSubCredentialsProvider(CredentialsProvider provider) {
        this.provider = provider;
    }

    @Override
    public CredentialsProvider getProvider() {
        return provider;
    }
}
