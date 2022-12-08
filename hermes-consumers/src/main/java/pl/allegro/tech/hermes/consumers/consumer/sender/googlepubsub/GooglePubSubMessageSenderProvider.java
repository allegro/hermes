package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.common.collect.ImmutableSet;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.ResilientMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.SingleRecipientMessageSenderAdapter;

import java.io.IOException;
import java.util.Set;

public class GooglePubSubMessageSenderProvider implements ProtocolMessageSenderProvider {

    public static final String SUPPORTED_PROTOCOL = "googlepubsub";

    private final GooglePubSubSenderTargetResolver resolver;
    private final GooglePubSubClientsPool clientsPool;

    public GooglePubSubMessageSenderProvider(GooglePubSubSenderTargetResolver resolver,
                                             CredentialsProvider credentialsProvider,
                                             ExecutorProvider executorProvider,
                                             RetrySettings retrySettings,
                                             BatchingSettings batchingSettings,
                                             TransportChannelProvider transportChannelProvider,
                                             GooglePubSubMessageTransformerCreator messageTransformerCreator) {

        this.resolver = resolver;
        this.clientsPool = new GooglePubSubClientsPool(
                credentialsProvider,
                executorProvider,
                retrySettings,
                batchingSettings,
                messageTransformerCreator,
                transportChannelProvider
        );
    }

    @Override
    public MessageSender create(final Subscription subscription, ResilientMessageSender resilientMessageSender) {
        final GooglePubSubSenderTarget resolvedTarget = resolver.resolve(subscription.getEndpoint());
        try {
            GooglePubSubMessageSender sender = new GooglePubSubMessageSender(resolvedTarget, clientsPool);
            return new SingleRecipientMessageSenderAdapter(sender, resilientMessageSender);
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
