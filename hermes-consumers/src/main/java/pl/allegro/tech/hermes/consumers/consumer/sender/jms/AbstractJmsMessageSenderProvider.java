package pl.allegro.tech.hermes.consumers.consumer.sender.jms;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.trace.TraceIdAppender;
import pl.allegro.tech.hermes.consumers.uri.UriUtils;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import java.net.URI;
import java.util.concurrent.ExecutionException;

public abstract class AbstractJmsMessageSenderProvider implements JmsMessageSenderProvider {

    protected final ConfigFactory configFactory;
    protected final LoadingCache<URI, ConnectionFactory> connectionFactoryCache;
    protected final JmsTraceIdAppender traceIdAppender;

    public AbstractJmsMessageSenderProvider(ConfigFactory configFactory, JmsTraceIdAppender traceIdAppender) {
        this.configFactory = configFactory;
        this.connectionFactoryCache = CacheBuilder.newBuilder().build(new ConnectionFactoryLoader());
        this.traceIdAppender = traceIdAppender;
    }

    @Override
    public MessageSender create(String endpoint) {
        URI endpointURI = URI.create(endpoint);
        ConnectionFactory connectionFactory = getConnectionFactory(endpointURI);
        JMSContext jmsContext = connectionFactory.createContext(
                UriUtils.extractUserNameFromUri(endpointURI),
                UriUtils.extractPasswordFromUri(endpointURI)
        );

        return new JmsMessageSender(jmsContext, extractTopicName(endpointURI), traceIdAppender);
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
        connectionFactoryCache.invalidateAll();
    }

    private ConnectionFactory getConnectionFactory(URI serverUri) {
        try {
            return connectionFactoryCache.get(serverUri);
        } catch (ExecutionException e) {
            throw new InternalProcessingException(
                    String.format("Unable to create connection factory for url %s", serverUri), e);
        }
    }

    private String extractTopicName(URI endpointURI) {
        return UriUtils.extractContextFromUri(endpointURI).replaceFirst("/", "");
    }

    protected class ConnectionFactoryLoader extends CacheLoader<URI, ConnectionFactory> {

        @Override
        public ConnectionFactory load(URI serverUri) throws Exception {
            return createConnectionFactory(serverUri);
        }
    }
}
