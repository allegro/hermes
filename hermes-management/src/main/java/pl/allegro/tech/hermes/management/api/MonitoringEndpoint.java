package pl.allegro.tech.hermes.management.api;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.ConsumerGroup;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.slf4j.LoggerFactory.getLogger;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

@Path("monitoring")
public class MonitoringEndpoint {

    private static final Logger logger = getLogger(MonitoringEndpoint.class);
    private final SubscriptionService subscriptionService;
    private final TopicService topicService;
    private final MultiDCAwareService multiDCAwareService;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    public MonitoringEndpoint(SubscriptionService subscriptionService,
                              TopicService topicService,
                              MultiDCAwareService multiDCAwareService) {
        this.subscriptionService = subscriptionService;
        this.topicService = topicService;
        this.multiDCAwareService = multiDCAwareService;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/consumergroup")
    public void monitorSubscriptionsPartitions() {
        logger.info("Get all subscription started");
        List<TopicSubscriptions> topicSubscriptions = getAllSubscriptions();
        logger.info("Get all subscription ended");
        List<List<ConsumerGroup>> consumerGroups = new ArrayList<>(Collections.emptyList());

        executorService.submit(() -> {
            try {
                logger.info("Monitoring started for {} topics and {} subscriptions", topicSubscriptions.size(),
                        (int) topicSubscriptions.stream().map(TopicSubscriptions::getSubscriptions).flatMap(List::stream).count());
                topicSubscriptions.forEach(topicSubscription -> topicSubscription.subscriptions
                        .forEach(subscription ->
                                consumerGroups.add(multiDCAwareService.describeConsumerGroups(topicSubscription.topic, subscription))));
            } catch (Exception e) {
                logger.error("Error in monitoring ", e);
            } finally {
                logger.info("Monitoring ended");
                executorService.shutdown();
            }
        });
    }

    private List<TopicSubscriptions> getAllSubscriptions() {
        List<TopicSubscriptions> subscriptions = new ArrayList<>();
        topicService.listQualifiedTopicNames().forEach(topic -> {
            TopicName topicName = fromQualifiedName(topic);
            subscriptions.add(new TopicSubscriptions(topicService.getTopicDetails(topicName),
                    subscriptionService.listSubscriptionNames(topicName)));
        });
        return subscriptions;
    }

    private static class TopicSubscriptions {
        private final Topic topic;
        private final List<String> subscriptions;

        public TopicSubscriptions(Topic topic, List<String> subscriptions) {
            this.topic = topic;
            this.subscriptions = subscriptions;
        }

        public Topic getTopic() {
            return topic;
        }

        public List<String> getSubscriptions() {
            return subscriptions;
        }
    }
}
