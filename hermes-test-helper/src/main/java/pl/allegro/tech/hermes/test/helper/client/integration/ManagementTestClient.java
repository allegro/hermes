package pl.allegro.tech.hermes.test.helper.client.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.UriBuilder;
import java.time.Duration;
import java.util.List;
import org.eclipse.jetty.client.HttpClient;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.MessageFiltersVerificationInput;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest;
import pl.allegro.tech.hermes.api.OffsetRetransmissionDate;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Readiness;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import reactor.core.publisher.Mono;

public class ManagementTestClient {

  private static final String TOPICS_PATH = "/topics";

  private static final String TOPIC_PATH = "/topics/{topicName}";

  private static final String SUBSCRIPTIONS_PATH = "/topics/{topicName}/subscriptions";

  private static final String SUBSCRIPTION_PATH =
      "/topics/{topicName}/subscriptions/{subscriptionName}";

  private static final String SUBSCRIPTION_STATE_PATH =
      "/topics/{topicName}/subscriptions/{subscriptionName}/state";

  private static final String SUBSCRIPTION_METRICS_PATH =
      "/topics/{topicName}/subscriptions/{subscriptionName}/metrics";

  private static final String GROUPS_PATH = "/groups";

  private static final String GROUP_PATH = "/groups/{groupName}";

  private static final String RETRANSMISSION_PATH =
      "/topics/{topicName}/subscriptions/{subscriptionName}/retransmission";

  private static final String BLACKLIST_TOPICS_PATH = "/blacklist/topics";

  private static final String BLACKLIST_TOPIC_PATH = "/blacklist/topics/{topicName}";

  private static final String LATEST_UNDELIVERED_MESSAGE =
      "/topics/{topicName}/subscriptions/{subscriptionName}/undelivered";

  private static final String TOPIC_PREVIEW = "/topics/{topicName}/preview";

  private static final String TOPIC_PREVIEW_OFFSET =
      "/topics/{topicName}/preview/cluster/{brokersClusterName}/partition/{partition}/offset/{offset}";

  private static final String MOVE_SUBSCRIPTION_OFFSETS =
      "/topics/{topicName}/subscriptions/{subscriptionName}/moveOffsetsToTheEnd";

  private static final String SET_READINESS = "/readiness/datacenters/{dc}";

  private static final String GET_READINESS = "/readiness/datacenters";

  private static final String TOPIC_SCHEMA = "/topics/{topicName}/schema";

  private static final String ALL_TOPIC_CLIENTS = "/topics/{topicName}/clients";

  private static final String SUBSCRIPTIONS_BY_OWNER = "/subscriptions/owner/{source}/{ownerId}";

  private static final String TOPICS_BY_OWNER = "/topics/owner/{source}/{ownerId}";

  private static final String TOPIC_METRICS_PATH = "/topics/{topicName}/metrics";

  private static final String FILTERS = "/filters/{topicName}";

  private static final String STATUS_HEALTH = "/status/health";

  private static final String STATS = "/stats";

  private static final String QUERY_GROUPS = "/query/groups";

  private static final String QUERY_TOPICS = "/query/topics";

  private static final String QUERY_SUBSCRIPTIONS = "/query/subscriptions";

  private static final String QUERY_TOPIC_METRICS = "/query/topics/metrics";

  private static final String QUERY_SUBSCRIPTION_METRICS = "/query/subscriptions/metrics";

  private static final String OAUTH_PROVIDERS_PATH = "/oauth/providers";

  private static final String SUBSCRIPTIONS_QUERY = "/topics/{topicName}/subscriptions/query";

  private static final String OAUTH_PROVIDER_PATH = "/oauth/providers/{oAuthProviderName}";

  private static final String UNHEALTHY_PATH = "/unhealthy";

  private static final String TOPICS_QUERY = "/topics/query";

  private static final String MODE = "/mode";

  private static final String OFFLINE_RETRANSMISSION_TASKS = "/offline-retransmission/tasks";

  private static final String OFFLINE_RETRANSMISSION_TASK =
      "/offline-retransmission/tasks/{taskId}";

  private static final String SUBSCRIPTION_HEALTH =
      "/topics/{topicName}/subscriptions/{subscription}/health";

  private static final String CONSUMER_GROUPS =
      "/topics/{topicName}/subscriptions/{subscription}/consumer-groups";

  private static final String OWNERS_SEARCH_PATH = "/owners/sources/{source}";

  private final WebTestClient webTestClient;

  private final String managementContainerUrl;

  private final ObjectMapper objectMapper;

  public ManagementTestClient(int managementPort) {
    this.managementContainerUrl = "http://localhost:" + managementPort;
    this.webTestClient = configureWebTestClient().build();
    this.objectMapper = new ObjectMapper();
  }

  public ManagementTestClient(
      int managementPort, String defaultHeaderName, String defaultHeaderValue) {
    this.managementContainerUrl = "http://localhost:" + managementPort;
    this.webTestClient =
        configureWebTestClient().defaultHeader(defaultHeaderName, defaultHeaderValue).build();
    this.objectMapper = new ObjectMapper();
  }

  private WebTestClient.Builder configureWebTestClient() {
    return WebTestClient.bindToServer(clientHttpConnector())
        .responseTimeout(Duration.ofSeconds(30))
        .baseUrl(managementContainerUrl)
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024));
  }

  private static ClientHttpConnector clientHttpConnector() {
    HttpClient httpClient = new HttpClient();
    httpClient.setMaxConnectionsPerDestination(256);
    return new JettyClientHttpConnector(httpClient);
  }

  public WebTestClient.ResponseSpec createGroup(Group group) {
    return sendCreateGroupRequest(group);
  }

  public List<String> getGroups() {
    String jsonString =
        webTestClient
            .get()
            .uri(GROUPS_PATH)
            .exchange()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    return mapStringJsonToListOfString(jsonString);
  }

  public WebTestClient.ResponseSpec createTopic(TopicWithSchema topicWithSchema) {
    return sendCreateTopicRequest(topicWithSchema);
  }

  public WebTestClient.ResponseSpec getTopic(String topicQualifiedName) {
    return getSingleTopic(topicQualifiedName);
  }

  public WebTestClient.ResponseSpec createSubscription(Subscription subscription) {
    return sendCreateSubscriptionRequest(subscription);
  }

  public WebTestClient.ResponseSpec updateSubscription(
      Topic topic, String subscription, PatchData patch) {
    return webTestClient
        .put()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(SUBSCRIPTION_PATH)
                .build(topic.getQualifiedName(), subscription))
        .body(Mono.just(patch), PatchData.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec updateSubscriptionState(
      Topic topic, String subscription, Subscription.State state) {
    return webTestClient
        .put()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(SUBSCRIPTION_STATE_PATH)
                .build(topic.getQualifiedName(), subscription))
        .body(Mono.just(state), Subscription.State.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec getSubscription(
      String topicQualifiedName, String subscriptionName) {
    return getSingleSubscription(topicQualifiedName, subscriptionName);
  }

  public WebTestClient.ResponseSpec getSubscriptionMetrics(
      String topicQualifiedName, String subscriptionName) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(SUBSCRIPTION_METRICS_PATH)
                .build(topicQualifiedName, subscriptionName))
        .exchange();
  }

  private WebTestClient.ResponseSpec getSingleTopic(String topicQualifiedName) {
    return webTestClient
        .get()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(TOPIC_PATH).build(topicQualifiedName))
        .exchange();
  }

  private WebTestClient.ResponseSpec getSingleSubscription(
      String topicQualifiedName, String subscriptionName) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(SUBSCRIPTION_PATH)
                .build(topicQualifiedName, subscriptionName))
        .exchange();
  }

  private WebTestClient.ResponseSpec sendCreateTopicRequest(TopicWithSchema topicWithSchema) {
    return webTestClient
        .post()
        .uri(TOPICS_PATH)
        .body(Mono.just(topicWithSchema), TopicWithSchema.class)
        .exchange();
  }

  WebTestClient.ResponseSpec retransmit(
      String topicName,
      String subscriptionName,
      OffsetRetransmissionDate retransmissionDate,
      boolean dryRun) {
    return webTestClient
        .put()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(RETRANSMISSION_PATH)
                .queryParam("dryRun", dryRun)
                .build(topicName, subscriptionName))
        .body(Mono.just(retransmissionDate), OffsetRetransmissionDate.class)
        .exchange();
  }

  private WebTestClient.ResponseSpec sendCreateSubscriptionRequest(Subscription subscription) {
    return webTestClient
        .post()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(SUBSCRIPTIONS_PATH)
                .build(subscription.getQualifiedTopicName()))
        .body(Mono.just(subscription), Subscription.class)
        .exchange();
  }

  private WebTestClient.ResponseSpec sendCreateGroupRequest(Group group) {
    return webTestClient.post().uri(GROUPS_PATH).body(Mono.just(group), Group.class).exchange();
  }

  private List<String> mapStringJsonToListOfString(String jsonString) {
    try {
      return objectMapper.readValue(jsonString, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public WebTestClient.ResponseSpec blacklistTopic(String topicQualifiedName) {
    return webTestClient
        .post()
        .uri(BLACKLIST_TOPICS_PATH)
        .body(Mono.just(List.of(topicQualifiedName)), List.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec unblacklistTopic(String topicQualifiedName) {
    return webTestClient
        .delete()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(BLACKLIST_TOPIC_PATH)
                .build(topicQualifiedName))
        .exchange();
  }

  public WebTestClient.ResponseSpec isTopicBlacklisted(String topicQualifiedName) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(BLACKLIST_TOPIC_PATH)
                .build(topicQualifiedName))
        .exchange();
  }

  public WebTestClient.ResponseSpec getLatestUndeliveredMessage(
      String topicQualifiedName, String subscriptionName) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(LATEST_UNDELIVERED_MESSAGE)
                .build(topicQualifiedName, subscriptionName))
        .exchange();
  }

  public WebTestClient.ResponseSpec getPreview(
      String qualifiedTopicName, String primaryKafkaClusterName, int partition, long offset) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(TOPIC_PREVIEW_OFFSET)
                .build(qualifiedTopicName, primaryKafkaClusterName, partition, offset))
        .exchange();
  }

  public WebTestClient.ResponseSpec getPreview(String qualifiedTopicName) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(TOPIC_PREVIEW)
                .build(qualifiedTopicName))
        .exchange();
  }

  public WebTestClient.ResponseSpec updateTopic(String qualifiedTopicName, PatchData patch) {
    return webTestClient
        .put()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(TOPIC_PATH).build(qualifiedTopicName))
        .body(Mono.just(patch), PatchData.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec getTopicMetrics(String qualifiedTopicName) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(TOPIC_METRICS_PATH)
                .build(qualifiedTopicName))
        .exchange();
  }

  public WebTestClient.ResponseSpec listSubscriptions(String qualifiedTopicName, boolean tracked) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(SUBSCRIPTIONS_PATH)
                .queryParam("tracked", tracked)
                .build(qualifiedTopicName))
        .exchange();
  }

  public WebTestClient.ResponseSpec listTopics(String groupName, boolean tracked) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(TOPICS_PATH)
                .queryParam("groupName", groupName)
                .queryParam("tracked", tracked)
                .build())
        .exchange();
  }

  public WebTestClient.ResponseSpec setReadiness(String dc, boolean state) {
    return webTestClient
        .post()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(SET_READINESS).build(dc))
        .body(Mono.just(new Readiness(state)), Readiness.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec getReadiness() {
    return webTestClient
        .get()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(GET_READINESS).build())
        .exchange();
  }

  public WebTestClient.ResponseSpec saveSchema(
      String qualifiedTopicName, boolean validate, String schema) {
    return webTestClient
        .post()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(TOPIC_SCHEMA)
                .queryParam("validate", validate)
                .build(qualifiedTopicName))
        .header("Content-Type", "application/json")
        .body(Mono.just(schema), String.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec saveSchema(String qualifiedTopicName, String schema) {
    return webTestClient
        .post()
        .uri(
            UriBuilder.fromUri(managementContainerUrl).path(TOPIC_SCHEMA).build(qualifiedTopicName))
        .header("Content-Type", "application/json")
        .body(Mono.just(schema), String.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec getSchema(String qualifiedTopicName) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl).path(TOPIC_SCHEMA).build(qualifiedTopicName))
        .exchange();
  }

  public WebTestClient.ResponseSpec deleteSchema(String qualifiedTopicName) {
    return webTestClient
        .delete()
        .uri(
            UriBuilder.fromUri(managementContainerUrl).path(TOPIC_SCHEMA).build(qualifiedTopicName))
        .exchange();
  }

  public WebTestClient.ResponseSpec verifyFilters(
      String qualifiedTopicName, MessageFiltersVerificationInput input) {
    return webTestClient
        .post()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(FILTERS).build(qualifiedTopicName))
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(input), MessageFiltersVerificationInput.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec getStatusHealth() {
    return webTestClient
        .get()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(STATUS_HEALTH).build())
        .exchange();
  }

  public WebTestClient.ResponseSpec getStats() {
    return webTestClient
        .get()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(STATS).build())
        .exchange();
  }

  public WebTestClient.ResponseSpec createOAuthProvider(OAuthProvider provider) {
    return webTestClient
        .post()
        .uri(OAUTH_PROVIDERS_PATH)
        .body(Mono.just(provider), OAuthProvider.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec getOAuthProvider(String name) {
    return webTestClient
        .get()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(OAUTH_PROVIDER_PATH).build(name))
        .exchange();
  }

  public WebTestClient.ResponseSpec removeOAuthProvider(String name) {
    return webTestClient
        .delete()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(OAUTH_PROVIDER_PATH).build(name))
        .exchange();
  }

  public WebTestClient.ResponseSpec listOAuthProvider() {
    return webTestClient
        .get()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(OAUTH_PROVIDERS_PATH).build())
        .exchange();
  }

  public WebTestClient.ResponseSpec updateOAuthProvider(String name, PatchData patch) {
    return webTestClient
        .put()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(OAUTH_PROVIDER_PATH).build(name))
        .body(Mono.just(patch), PatchData.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec getAllTopicClients(String topicQualifiedName) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(ALL_TOPIC_CLIENTS)
                .build(topicQualifiedName))
        .exchange();
  }

  public WebTestClient.ResponseSpec getSubscriptionsForOwner(String source, String ownerId) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(SUBSCRIPTIONS_BY_OWNER)
                .build(source, ownerId))
        .exchange();
  }

  public WebTestClient.ResponseSpec deleteSubscription(
      String topicQualifiedName, String subscriptionName) {
    return webTestClient
        .delete()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(SUBSCRIPTION_PATH)
                .build(topicQualifiedName, subscriptionName))
        .exchange();
  }

  public WebTestClient.ResponseSpec getTopicsForOwner(String source, String ownerId) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl).path(TOPICS_BY_OWNER).build(source, ownerId))
        .exchange();
  }

  public WebTestClient.ResponseSpec deleteTopic(String topicQualifiedName) {
    return webTestClient
        .delete()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(TOPIC_PATH).build(topicQualifiedName))
        .exchange();
  }

  public WebTestClient.ResponseSpec queryTopics(String group, String query) {
    return webTestClient
        .post()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(TOPICS_QUERY)
                .queryParam("groupName", group)
                .build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(query), String.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec queryGroups(String query) {
    return webTestClient
        .post()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(QUERY_GROUPS).build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(query), String.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec queryTopics(String query) {
    return webTestClient
        .post()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(QUERY_TOPICS).build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(query), String.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec queryTopicMetrics(String query) {
    return webTestClient
        .post()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(QUERY_TOPIC_METRICS).build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(query), String.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec querySubscriptionMetrics(String query) {
    return webTestClient
        .post()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(QUERY_SUBSCRIPTION_METRICS).build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(query), String.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec querySubscriptions(String query) {
    return webTestClient
        .post()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(QUERY_SUBSCRIPTIONS).build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(query), String.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec listUnhealthy() {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(UNHEALTHY_PATH)
                .queryParam("respectMonitoringSeverity", "false")
                .build())
        .header("Accept", "application/json")
        .exchange();
  }

  public WebTestClient.ResponseSpec listUnhealthyAsPlainText() {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(UNHEALTHY_PATH)
                .queryParam("respectMonitoringSeverity", "false")
                .build())
        .header("Accept", "text/plain")
        .exchange();
  }

  public WebTestClient.ResponseSpec listUnhealthy(String ownerId) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(UNHEALTHY_PATH)
                .queryParam("ownerSourceName", "Plaintext")
                .queryParam("ownerId", ownerId)
                .queryParam("respectMonitoringSeverity", "false")
                .build())
        .header("Accept", "application/json")
        .exchange();
  }

  public WebTestClient.ResponseSpec listUnhealthyAsPlainText(String ownerId) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(UNHEALTHY_PATH)
                .queryParam("ownerSourceName", "Plaintext")
                .queryParam("ownerId", ownerId)
                .queryParam("respectMonitoringSeverity", "false")
                .build())
        .header("Accept", "text/plain")
        .exchange();
  }

  public WebTestClient.ResponseSpec listUnhealthyForTopic(String qualifiedName) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(UNHEALTHY_PATH)
                .queryParam("qualifiedTopicNames", qualifiedName)
                .queryParam("respectMonitoringSeverity", "false")
                .build())
        .header("Accept", "application/json")
        .exchange();
  }

  public WebTestClient.ResponseSpec listUnhealthyForTopicAsPlainText(String qualifiedName) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(UNHEALTHY_PATH)
                .queryParam("qualifiedTopicNames", qualifiedName)
                .build())
        .header("Accept", "text/plain")
        .exchange();
  }

  public WebTestClient.ResponseSpec listUnhealthyForSubscription(
      String topicQualifiedName, String subscriptionName) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(UNHEALTHY_PATH)
                .queryParam("subscriptionNames", subscriptionName)
                .queryParam("qualifiedTopicNames", topicQualifiedName)
                .queryParam("respectMonitoringSeverity", "false")
                .build())
        .header("Accept", "application/json")
        .exchange();
  }

  public WebTestClient.ResponseSpec listUnhealthyForSubscriptionAsPlainText(
      String topicQualifiedName, String subscriptionName) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(UNHEALTHY_PATH)
                .queryParam("subscriptionNames", subscriptionName)
                .queryParam("qualifiedTopicNames", topicQualifiedName)
                .queryParam("respectMonitoringSeverity", "false")
                .build())
        .header("Accept", "text/plain")
        .exchange();
  }

  public WebTestClient.ResponseSpec searchOwners(String source, String searchString) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(OWNERS_SEARCH_PATH)
                .queryParam("search", searchString)
                .build(source))
        .exchange();
  }

  public WebTestClient.ResponseSpec setMode(String mode) {
    return webTestClient
        .post()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(MODE).queryParam("mode", mode).build())
        .exchange();
  }

  public WebTestClient.ResponseSpec getOfflineRetransmissionTasks() {
    return webTestClient
        .get()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(OFFLINE_RETRANSMISSION_TASKS).build())
        .exchange();
  }

  public WebTestClient.ResponseSpec deleteOfflineRetransmissionTask(String taskId) {
    return webTestClient
        .delete()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(OFFLINE_RETRANSMISSION_TASK)
                .build(taskId))
        .exchange();
  }

  public WebTestClient.ResponseSpec createOfflineRetransmissionTask(
      OfflineRetransmissionRequest request) {
    return webTestClient
        .post()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(OFFLINE_RETRANSMISSION_TASKS).build())
        .header("Content-Type", "application/json")
        .body(Mono.just(request), OfflineRetransmissionRequest.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec querySubscriptions(String qualifiedTopicName, String query) {
    return webTestClient
        .post()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(SUBSCRIPTIONS_QUERY)
                .build(qualifiedTopicName))
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(query), String.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec getSubscriptionHealth(String qualifiedTopicName, String name) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(SUBSCRIPTION_HEALTH)
                .build(qualifiedTopicName, name))
        .exchange();
  }

  public WebTestClient.ResponseSpec getConsumerGroupsDescription(
      String qualifiedTopicName, String subscriptionName) {
    return webTestClient
        .get()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(CONSUMER_GROUPS)
                .build(qualifiedTopicName, subscriptionName))
        .exchange();
  }

  public WebTestClient.ResponseSpec deleteGroup(String groupName) {
    return webTestClient
        .delete()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(GROUP_PATH).build(groupName))
        .exchange();
  }

  public WebTestClient.ResponseSpec updateGroup(String groupName, Group group) {
    return webTestClient
        .put()
        .uri(UriBuilder.fromUri(managementContainerUrl).path(GROUP_PATH).build(groupName))
        .body(Mono.just(group), Group.class)
        .exchange();
  }

  public WebTestClient.ResponseSpec moveOffsetsToTheEnd(
      String topicQualifiedName, String subscriptionName) {
    return webTestClient
        .post()
        .uri(
            UriBuilder.fromUri(managementContainerUrl)
                .path(MOVE_SUBSCRIPTION_OFFSETS)
                .build(topicQualifiedName, subscriptionName))
        .exchange();
  }
}
