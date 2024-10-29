package pl.allegro.tech.hermes.integrationtests.setup;

import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.integrationtests.setup.HermesManagementTestApp.AUDIT_EVENT_PATH;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.lifecycle.Startable;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.group.GroupNotEmptyException;
import pl.allegro.tech.hermes.env.BrokerOperations;
import pl.allegro.tech.hermes.integrationtests.prometheus.PrometheusExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.group.GroupService;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.test.helper.client.integration.HermesInitHelper;
import pl.allegro.tech.hermes.test.helper.client.integration.HermesTestClient;
import pl.allegro.tech.hermes.test.helper.containers.ConfluentSchemaRegistryContainer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;
import pl.allegro.tech.hermes.test.helper.environment.HermesTestApp;

public class HermesExtension
    implements BeforeAllCallback, AfterAllCallback, ExtensionContext.Store.CloseableResource {

  private static final Logger logger = LoggerFactory.getLogger(HermesExtension.class);

  public static final TestSubscribersExtension auditEventsReceiver = new TestSubscribersExtension();

  private static final ZookeeperContainer hermesZookeeper =
      new ZookeeperContainer("HermesZookeeper");
  private static final KafkaContainerCluster kafka = new KafkaContainerCluster(1);
  public static final ConfluentSchemaRegistryContainer schemaRegistry =
      new ConfluentSchemaRegistryContainer().withKafkaCluster(kafka);
  private static final HermesConsumersTestApp consumers =
      new HermesConsumersTestApp(hermesZookeeper, kafka, schemaRegistry);
  private static final HermesManagementTestApp management =
      new HermesManagementTestApp(hermesZookeeper, kafka, schemaRegistry);
  private static final HermesFrontendTestApp frontend =
      new HermesFrontendTestApp(hermesZookeeper, kafka, schemaRegistry);
  private HermesTestClient hermesTestClient;
  private HermesInitHelper hermesInitHelper;
  private static final RequestUser testUser = new TestUser();

  private static boolean started = false;

  public static TestSubscriber auditEvents;

  public static BrokerOperations brokerOperations;

  @Override
  public void beforeAll(ExtensionContext context) {
    if (!started) {
      Stream.of(hermesZookeeper, kafka).parallel().forEach(Startable::start);
      schemaRegistry.start();
      management.addEventAuditorListener(auditEventsReceiver.getPort());
      management.start();
      Stream.of(consumers, frontend).forEach(HermesTestApp::start);
      started = true;
    }
    Stream.of(management, consumers, frontend)
        .forEach(
            app -> {
              if (app.shouldBeRestarted()) {
                app.stop();
                app.start();
              }
            });
    hermesTestClient =
        new HermesTestClient(management.getPort(), frontend.getPort(), consumers.getPort());
    hermesInitHelper = new HermesInitHelper(management.getPort());
    auditEvents = auditEventsReceiver.createSubscriberWithStrictPath(200, AUDIT_EVENT_PATH);
    brokerOperations =
        new BrokerOperations(kafka.getBootstrapServersForExternalClients(), "itTest");
  }

  @Override
  public void close() {
    Stream.of(management, consumers, frontend).parallel().forEach(HermesTestApp::stop);
    Stream.of(hermesZookeeper, kafka, schemaRegistry).parallel().forEach(Startable::stop);
    started = false;
  }

  public int getFrontendPort() {
    return frontend.getPort();
  }

  public HermesTestClient api() {
    return hermesTestClient;
  }

  public HermesInitHelper initHelper() {
    return hermesInitHelper;
  }

  public void cutOffConnectionsBetweenBrokersAndClients() {
    kafka.cutOffConnectionsBetweenBrokersAndClients();
  }

  public void restoreConnectionsBetweenBrokersAndClients() {
    kafka.restoreConnectionsBetweenBrokersAndClients();
  }

  private void removeSubscriptions() {
    SubscriptionService service = management.subscriptionService();
    List<Subscription> subscriptions = service.getAllSubscriptions();
    for (Subscription subscription : subscriptions) {
      service.removeSubscription(subscription.getTopicName(), subscription.getName(), testUser);
    }

    waitAtMost(adjust(Duration.ofMinutes(1)))
        .untilAsserted(
            () -> Assertions.assertThat(service.getAllSubscriptions().size()).isEqualTo(0));
  }

  private void removeTopics() {
    TopicService service = management.topicService();
    List<Topic> topics = service.getAllTopics();
    for (Topic topic : topics) {
      service.removeTopicWithSchema(topic, testUser);
    }

    waitAtMost(adjust(Duration.ofMinutes(1)))
        .untilAsserted(() -> Assertions.assertThat(service.getAllTopics().size()).isEqualTo(0));
  }

  private void removeGroups() {
    GroupService service = management.groupService();
    List<Group> groups = service.listGroups();
    for (Group group : groups) {
      try {
        service.removeGroup(group.getGroupName(), testUser);
      } catch (GroupNotEmptyException e) {
        logger.warn("Error during removing group: {}", group, e);
      }
    }
  }

  public void clearManagementData() {
    removeSubscriptions();
    removeTopics();
    removeGroups();
  }

  public HermesExtension withPrometheus(PrometheusExtension prometheus) {
    management.withPrometheus(prometheus);
    return this;
  }

  public HermesExtension withGooglePubSub(GooglePubSubExtension googlePubSub) {
    consumers.withGooglePubSubEndpoint(googlePubSub);
    return this;
  }

  public HermesExtension withFrontendProperty(String name, Object value) {
    frontend.withProperty(name, value);
    return this;
  }

  public HermesExtension withFrontendProfile(String profile) {
    frontend.withSpringProfile(profile);
    return this;
  }

  @Override
  public void afterAll(ExtensionContext context) {
    Stream.of(management, consumers, frontend).forEach(HermesTestApp::restoreDefaultSettings);
  }
}
