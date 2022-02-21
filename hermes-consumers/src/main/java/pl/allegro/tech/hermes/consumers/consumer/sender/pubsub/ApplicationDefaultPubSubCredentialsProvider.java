package pl.allegro.tech.hermes.consumers.consumer.sender.pubsub;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;

public class ApplicationDefaultPubSubCredentialsProvider implements PubSubCredentialsProvider {

    private FixedCredentialsProvider fixedCredentialsProvider;

    @Override
    public CredentialsProvider getProvider() throws IOException {
        if (fixedCredentialsProvider == null) {
            fixedCredentialsProvider = FixedCredentialsProvider.create(GoogleCredentials.getApplicationDefault());
        }

        return fixedCredentialsProvider;
    }
}
