package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import pl.allegro.tech.hermes.consumers.consumer.sender.SenderClientsPool;

import java.io.IOException;

class GooglePubSubClientsPool extends SenderClientsPool<GooglePubSubSenderTarget, GooglePubSubClient> {

    private final CredentialsProvider credentialsProvider;
    private final ExecutorProvider publishingExecutorProvider;
    private final RetrySettings retrySettings;
    private final BatchingSettings batchingSettings;
    private final TransportChannelProvider transportChannelProvider;

    GooglePubSubClientsPool(CredentialsProvider credentialsProvider,
                            ExecutorProvider publishingExecutorProvider,
                            RetrySettings retrySettings,
                            BatchingSettings batchingSettings,
                            TransportChannelProvider transportChannelProvider) {
        this.credentialsProvider = credentialsProvider;
        this.publishingExecutorProvider = publishingExecutorProvider;
        this.retrySettings = retrySettings;
        this.batchingSettings = batchingSettings;
        this.transportChannelProvider = transportChannelProvider;
    }

    protected GooglePubSubClient createClient(GooglePubSubSenderTarget resolvedTarget) throws IOException {
        final Publisher.Builder builder = Publisher.newBuilder(resolvedTarget.getTopicName())
                .setEndpoint(resolvedTarget.getPubSubEndpoint())
                .setCredentialsProvider(credentialsProvider)
                .setRetrySettings(retrySettings)
                .setBatchingSettings(batchingSettings)
                .setExecutorProvider(publishingExecutorProvider);

        Publisher publisher;
        if (transportChannelProvider == null) {
            publisher = builder.build();
        } else {
            publisher = builder.setChannelProvider(transportChannelProvider).build();
        }
        return new GooglePubSubClient(publisher);
    }
}
