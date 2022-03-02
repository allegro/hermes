package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.cache.GooglePubSubPublishersCache;

import java.util.Set;
import java.util.concurrent.ExecutionException;

public class GooglePubSubMessageSenderProvider implements ProtocolMessageSenderProvider {

    public static final String SUPPORTED_PROTOCOL = "googlepubsub";

    private static final Logger logger = LoggerFactory.getLogger(GooglePubSubMessageSenderProvider.class);

    private final GooglePubSubSenderTargetResolver resolver;
    private final GooglePubSubMessages messageCreator;
    private final GooglePubSubPublishersCache publishersCache;

    public GooglePubSubMessageSenderProvider(GooglePubSubSenderTargetResolver resolver,
                                             GooglePubSubPublishersCache publishersCache,
                                             GooglePubSubMessages messageCreator) {

        this.resolver = resolver;
        this.messageCreator = messageCreator;
        this.publishersCache = publishersCache;
    }

    @Override
    public MessageSender create(final Subscription subscription) {
        final GooglePubSubSenderTarget resolvedTarget = resolver.resolve(subscription.getEndpoint());
        Publisher publisher = null;
        try {
            publisher = publishersCache.get(resolvedTarget);
        } catch (ExecutionException e) {
            logger.warn("Cannot create Google PubSub publishers cache", e);
        }
        Preconditions.checkNotNull(publisher, "PubSub Publisher cannot be null");

        return new GooglePubSubMessageSender(new GooglePubSubClient(publisher, messageCreator));
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
        publishersCache.shutdown();
    }
}
