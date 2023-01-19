package pl.allegro.tech.hermes.management.api;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.ConsumerGroup;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.ClustersProvider;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService2;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService3;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService4;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService5;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService6;

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
    private final MultiDCAwareService2 multiDCAwareService2;
    private final MultiDCAwareService3 multiDCAwareService3;
    private final MultiDCAwareService4 multiDCAwareService4;
    private final MultiDCAwareService5 multiDCAwareService5;
    private final MultiDCAwareService6 multiDCAwareService6;
    private final ClustersProvider clustersProvider;
    private final ExecutorService executorService = Executors.newFixedThreadPool(6);

    @Autowired
    public MonitoringEndpoint(SubscriptionService subscriptionService,
                              TopicService topicService,
                              MultiDCAwareService multiDCAwareService, MultiDCAwareService2 multiDCAwareService2, MultiDCAwareService3 multiDCAwareService3, MultiDCAwareService4 multiDCAwareService4, MultiDCAwareService5 multiDCAwareService5, MultiDCAwareService6 multiDCAwareService6, ClustersProvider clustersProvider) {
        this.subscriptionService = subscriptionService;
        this.topicService = topicService;
        this.multiDCAwareService = multiDCAwareService;
        this.multiDCAwareService2 = multiDCAwareService2;
        this.multiDCAwareService3 = multiDCAwareService3;
        this.multiDCAwareService4 = multiDCAwareService4;
        this.multiDCAwareService5 = multiDCAwareService5;
        this.multiDCAwareService6 = multiDCAwareService6;
        this.clustersProvider = clustersProvider;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/consumergroup")
    public void monitorSubscriptionsPartitions() {
        logger.info("Get all subscription started");
        List<TopicSubscriptions> topicSubscriptions = getAllSubscriptions();
        logger.info("Get all subscription ended");
        List<List<ConsumerGroup>> consumerGroups = new ArrayList<>(Collections.emptyList());

        List<TopicSubscriptions> first = topicSubscriptions.subList(0, topicSubscriptions.size() / 6);
        List<TopicSubscriptions> second = topicSubscriptions.subList(topicSubscriptions.size() / 6, topicSubscriptions.size() / 6 * 2);
        List<TopicSubscriptions> third = topicSubscriptions.subList(topicSubscriptions.size() / 6 * 2, topicSubscriptions.size() / 6 * 3);
        List<TopicSubscriptions> fourth = topicSubscriptions.subList(topicSubscriptions.size() / 6 * 3, topicSubscriptions.size() / 6 * 4);
        List<TopicSubscriptions> fifth = topicSubscriptions.subList(topicSubscriptions.size() / 6 * 4, topicSubscriptions.size() / 6 * 5);
        List<TopicSubscriptions> sixth = topicSubscriptions.subList(topicSubscriptions.size() / 6 * 5, topicSubscriptions.size());

        executorService.submit(() -> {
            try {
                logger.info("Monitoring 1 started for {} topics and {} subscriptions", first.size(),
                        (int) first.stream().map(TopicSubscriptions::getSubscriptions).flatMap(List::stream).count());

                first.forEach(topicSubscription -> topicSubscription.subscriptions
                        .forEach(subscription ->
                                consumerGroups.add(multiDCAwareService.describeConsumerGroups(topicSubscription.topic, subscription))));
            } catch (Exception e) {
                logger.error("Error in monitoring 1 ", e);
            } finally {
                logger.info("Monitoring 1 ended");
                executorService.shutdown();
            }
        });

        executorService.submit(() -> {
            try {
                logger.info("Monitoring 2 started for {} topics and {} subscriptions", second.size(),
                        (int) second.stream().map(TopicSubscriptions::getSubscriptions).flatMap(List::stream).count());

                second.forEach(topicSubscription -> topicSubscription.subscriptions
                        .forEach(subscription ->
                                consumerGroups.add(multiDCAwareService2.describeConsumerGroups(topicSubscription.topic, subscription))));
            } catch (Exception e) {
                logger.error("Error in monitoring 2 ", e);
            } finally {
                logger.info("Monitoring 2 ended");
                executorService.shutdown();
            }
        });

        executorService.submit(() -> {
            try {
                logger.info("Monitoring 3 started for {} topics and {} subscriptions", third.size(),
                        (int) third.stream().map(TopicSubscriptions::getSubscriptions).flatMap(List::stream).count());

                third.forEach(topicSubscription -> topicSubscription.subscriptions
                        .forEach(subscription ->
                                consumerGroups.add(multiDCAwareService3.describeConsumerGroups(topicSubscription.topic, subscription))));
            } catch (Exception e) {
                logger.error("Error in monitoring 3 ", e);
            } finally {
                logger.info("Monitoring 3 ended");
                executorService.shutdown();
            }
        });

        executorService.submit(() -> {
            try {
                logger.info("Monitoring 4 started for {} topics and {} subscriptions", fourth.size(),
                        (int) fourth.stream().map(TopicSubscriptions::getSubscriptions).flatMap(List::stream).count());

                fourth.forEach(topicSubscription -> topicSubscription.subscriptions
                        .forEach(subscription ->
                                consumerGroups.add(multiDCAwareService4.describeConsumerGroups(topicSubscription.topic, subscription))));
            } catch (Exception e) {
                logger.error("Error in monitoring 4 ", e);
            } finally {
                logger.info("Monitoring 4 ended");
                executorService.shutdown();
            }
        });

        executorService.submit(() -> {
            try {
                logger.info("Monitoring 5 started for {} topics and {} subscriptions", fifth.size(),
                        (int) fifth.stream().map(TopicSubscriptions::getSubscriptions).flatMap(List::stream).count());

                fifth.forEach(topicSubscription -> topicSubscription.subscriptions
                        .forEach(subscription ->
                                consumerGroups.add(multiDCAwareService5.describeConsumerGroups(topicSubscription.topic, subscription))));
            } catch (Exception e) {
                logger.error("Error in monitoring 5 ", e);
            } finally {
                logger.info("Monitoring 5 ended");
                executorService.shutdown();
            }
        });

        executorService.submit(() -> {
            try {
                logger.info("Monitoring 6 started for {} topics and {} subscriptions", sixth.size(),
                        (int) sixth.stream().map(TopicSubscriptions::getSubscriptions).flatMap(List::stream).count());

                sixth.forEach(topicSubscription -> topicSubscription.subscriptions
                        .forEach(subscription ->
                                consumerGroups.add(multiDCAwareService6.describeConsumerGroups(topicSubscription.topic, subscription))));
            } catch (Exception e) {
                logger.error("Error in monitoring 6 ", e);
            } finally {
                logger.info("Monitoring 6 ended");
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
