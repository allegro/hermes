package pl.allegro.tech.hermes.consumers.consumer.sender.jms;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.Message;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.consumers.consumer.ResilientMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.SingleRecipientMessageSenderAdapter;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;
import pl.allegro.tech.hermes.consumers.uri.UriUtils;

public abstract class AbstractJmsMessageSenderProvider implements JmsMessageSenderProvider {

  protected final LoadingCache<URI, ConnectionFactory> connectionFactoryCache;
  protected final MetadataAppender<Message> metadataAppender;

  public AbstractJmsMessageSenderProvider(MetadataAppender<Message> metadataAppender) {
    this.connectionFactoryCache = CacheBuilder.newBuilder().build(new ConnectionFactoryLoader());
    this.metadataAppender = metadataAppender;
  }

  @Override
  public MessageSender create(
      Subscription subscription, ResilientMessageSender resilientMessageSender) {
    EndpointAddress endpoint = subscription.getEndpoint();
    URI uri = endpoint.getUri();
    ConnectionFactory connectionFactory = getConnectionFactory(uri);
    JMSContext jmsContext =
        connectionFactory.createContext(endpoint.getUsername(), endpoint.getPassword());

    JmsMessageSender jmsMessageSender =
        new JmsMessageSender(jmsContext, extractTopicName(uri), metadataAppender);
    return new SingleRecipientMessageSenderAdapter(jmsMessageSender, resilientMessageSender);
  }

  @Override
  public void start() throws Exception {}

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
