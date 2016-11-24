package pl.allegro.tech.hermes.test.helper.client;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import pl.allegro.tech.hermes.api.endpoints.*;
import pl.allegro.tech.hermes.consumers.ConsumerEndpoint;

import javax.ws.rs.Path;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

public class Hermes {

    public static final int DEFAULT_THREAD_POOL_SIZE = 10;
    public static final int DEFAULT_CONNECT_TIMEOUT = 500;
    public static final int DEFAULT_PUBLISHER_READ_TIMEOUT = 400;
    public static final int DEFAULT_MANAGEMENT_READ_TIMEOUT = 1000;

    private final String url;
    private final String consumerUrl;
    private ClientConfig managementConfig = getDefaultManagementConfig();
    private ClientConfig publisherConfig = getDefaultPublisherConfig();

    private Collection<ClientRequestFilter> filters = new ArrayList<>();

    public Hermes(String url, String consumerUrl) {
        this.url = url;
        this.consumerUrl = consumerUrl;
    }

    public Hermes withPassword(String password) {
        this.filters.add(new PasswordAuthenticationFeature(password));
        return this;
    }

    public Hermes withAuthToken(String authToken) {
        this.filters.add(new OAuth2AuthenticationFeature(clientRequestContext -> authToken));
        return this;
    }

    public Hermes withAuthToken(Function<ClientRequestContext, String> authTokenSupplier) {
        this.filters.add(new OAuth2AuthenticationFeature(authTokenSupplier));
        return this;
    }

    public Hermes withManagementConfig(ClientConfig config) {
        this.managementConfig = config;
        return this;
    }

    public Hermes withPublisherConfig(ClientConfig config) {
        this.publisherConfig = config;
        return this;
    }

    public GroupEndpoint createGroupEndpoint() {
        return createProxy(url, GroupEndpoint.class, managementConfig);
    }

    public TopicEndpoint createTopicEndpoint() {
        return createProxy(url, TopicEndpoint.class, managementConfig);
    }

    public SchemaEndpoint createSchemaEndpoint() {
        return createProxy(url, SchemaEndpoint.class, managementConfig);
    }

    public SubscriptionEndpoint createSubscriptionEndpoint() {
        return createProxy(url, SubscriptionEndpoint.class, managementConfig);
    }

    public QueryEndpoint createQueryEndpoint() {
        return createProxy(url, QueryEndpoint.class, managementConfig);
    }

    public OAuthProviderEndpoint createOAuthProviderEndpoint() {
        return createProxy(url, OAuthProviderEndpoint.class, managementConfig);
    }

    public ConsumerEndpoint createConsumerEndpoint() {
        return createProxy(consumerUrl, ConsumerEndpoint.class, getDefaultManagementConfig());
    }

    public SupportTeamsEndpoint createSupportTeamsEndpoint() {
        return createProxy(url, SupportTeamsEndpoint.class, managementConfig);
    }

    public BlacklistEndpoint createBlacklistEndpoint() {
        return createProxy(url, BlacklistEndpoint.class, managementConfig);
    }

    public AsyncMessagePublisher createAsyncMessagePublisher() {
        String resource = TopicEndpoint.class.getAnnotation(Path.class).value();
        return new AsyncMessagePublisher(getClientBuilder(publisherConfig).build().target(url).path(resource));
    }

    public WebTarget createWebTargetForPublishing() {
        String resource = TopicEndpoint.class.getAnnotation(Path.class).value();
        return getClientBuilder(publisherConfig).build().target(url).path(resource);
    }

    private <T> T createProxy(String url, Class<T> endpoint, ClientConfig clientConfig) {
        ClientBuilder clientBuilder = getClientBuilder(clientConfig);
        for (ClientRequestFilter filter : filters) {
            clientBuilder.register(filter);
        }
        return WebResourceFactory.newResource(endpoint, clientBuilder.build().target(url));
    }

    private static ClientConfig getDefaultManagementConfig() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_MANAGEMENT_READ_TIMEOUT);
        clientConfig.property(ClientProperties.READ_TIMEOUT, DEFAULT_MANAGEMENT_READ_TIMEOUT);

        return clientConfig;
    }

    private static ClientConfig getDefaultPublisherConfig() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ClientProperties.ASYNC_THREADPOOL_SIZE, DEFAULT_THREAD_POOL_SIZE);
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT);
        clientConfig.property(ClientProperties.READ_TIMEOUT, DEFAULT_PUBLISHER_READ_TIMEOUT);

        return clientConfig;
    }

    private static ClientBuilder getClientBuilder(ClientConfig clientConfig) {
        return ClientBuilder.newBuilder().withConfig(clientConfig).register(JacksonJsonProvider.class);
    }
}
