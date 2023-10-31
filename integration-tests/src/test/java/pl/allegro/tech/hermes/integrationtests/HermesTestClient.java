package pl.allegro.tech.hermes.integrationtests;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionMode;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.api.endpoints.TopicEndpoint;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;

import java.util.Map;
import java.util.UUID;

import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;
import static pl.allegro.tech.hermes.test.helper.client.Hermes.getClientBuilder;
import static pl.allegro.tech.hermes.test.helper.client.Hermes.getDefaultPublisherConfig;

public class HermesTestClient {

    protected final HermesEndpoints endpoints;

    private WebTarget webTarget;

    public HermesTestClient(HermesEndpoints endpoints, String frontendUrl) {
        this.endpoints = endpoints;
        String resource = TopicEndpoint.class.getAnnotation(Path.class).value();
        this.webTarget = getClientBuilder(getDefaultPublisherConfig()).build().target(frontendUrl).path(resource);
    }

    // TODO: should replace this name with createTopicWithRandomName?
    // TODO: Include human-readable name. It can be a prefix provided by the developer or test method name.
    public Topic createRandomTopic() {
        Topic topic = topic(UUID.randomUUID().toString(), UUID.randomUUID().toString()).build();
        endpoints.topic().create(TopicWithSchema.topicWithSchema(topic));
        return topic;
    }

    public void createRandomSubscription(Topic topic, String endpoint) {
        Subscription subscription = subscription(topic, UUID.randomUUID().toString())
                .withEndpoint(endpoint)
                .withContentType(ContentType.JSON)
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults().build())
                .withMode(SubscriptionMode.ANYCAST)
                .withState(Subscription.State.ACTIVE)
                .build();
        endpoints.subscription().create(topic.getQualifiedName(), subscription);
    }

    public void createSubscription(Topic topic, Subscription subscription) {

    }

    public Response publish(String qualifiedName, String body) {
        return webTarget.path(qualifiedName).request().headers(new MultivaluedHashMap<>(Map.of("Content-Type", MediaType.TEXT_PLAIN)))
                .post(Entity.entity(body, MediaType.TEXT_PLAIN));
    }

}
