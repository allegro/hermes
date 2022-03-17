package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.common.collect.ImmutableSet;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.auth.GooglePubSubCredentialsProvider;

import java.io.IOException;
import java.util.Set;

public class GooglePubSubMessageSenderProvider implements ProtocolMessageSenderProvider {

    public static final String SUPPORTED_PROTOCOL = "googlepubsub";

    private final GooglePubSubSenderTargetResolver resolver;
    private final GooglePubSubClientsPool clientsPool;

    public GooglePubSubMessageSenderProvider(GooglePubSubSenderTargetResolver resolver,
                                             GooglePubSubCredentialsProvider credentialsProvider,
                                             ExecutorProvider executorProvider,
                                             RetrySettings retrySettings,
                                             BatchingSettings batchingSettings,
                                             TransportChannelProvider transportChannelProvider,
                                             GooglePubSubMessages pubSubMessages) throws IOException {

        this.resolver = resolver;
        this.clientsPool = new GooglePubSubClientsPool(
                credentialsProvider.getProvider(),
                executorProvider,
                retrySettings,
                batchingSettings,
                pubSubMessages,
                transportChannelProvider
        );
    }

    @Override
    public MessageSender create(final Subscription subscription) {
        final GooglePubSubSenderTarget resolvedTarget = resolver.resolve(subscription.getEndpoint());
        try {
            return new GooglePubSubMessageSender(resolvedTarget, clientsPool);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create Google PubSub publishers cache", e);
        }
    }

    @Override
    public Set<String> getSupportedProtocols() {
        return ImmutableSet.of(SUPPORTED_PROTOCOL);
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
        clientsPool.shutdown();
    }
}
