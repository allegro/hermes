package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.message.undelivered.ZookeeperUndeliveredMessageLog;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClientManager;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class DistributedZookeeperUndeliveredMessageLog extends DistributedZookeeperRepository
        implements UndeliveredMessageLog {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperUndeliveredMessageLog.class);

    private static final String NODE_NAME = "undelivered";

    private final ConcurrentMap<SubscriptionName, SentMessageTrace> lastUndeliveredMessages = new ConcurrentHashMap<>();
    private final ZookeeperPaths paths;
    private final ExecutorService queryExecutor;

    public DistributedZookeeperUndeliveredMessageLog(ZookeeperClientManager clientManager,
                                                     ZookeeperPaths paths,
                                                     ExecutorService queryExecutor,
                                                     ObjectMapper mapper) {
        super(clientManager, mapper);
        this.paths = paths;
        this.queryExecutor = queryExecutor;
    }

    @Override
    public void add(SentMessageTrace message) {
        lastUndeliveredMessages.put(new SubscriptionName(message.getSubscription(), message.getTopicName()), message);
    }

    @Override
    public void persist() {
        for (SubscriptionName key : lastUndeliveredMessages.keySet()) {
            log(lastUndeliveredMessages.remove(key));
        }
    }

    private void log(SentMessageTrace messageTrace) {
        ZookeeperClient client = clientManager.getLocalClient();

        String undeliveredPath = paths.subscriptionPath(messageTrace.getTopicName(), messageTrace.getSubscription(), NODE_NAME);
        try {
            client.upsert(undeliveredPath, mapper.writeValueAsBytes(messageTrace));
        } catch (Exception e) {
            logger.warn(
                    format("Could not log undelivered message for topic: %s and subscription: %s",
                            messageTrace.getQualifiedTopicName(),
                            messageTrace.getSubscription()),
                    e);
        }
    }

    @Override
    public Optional<SentMessageTrace> last(TopicName topicName, String subscriptionName) {
        String tracePath = paths.subscriptionPath(topicName, subscriptionName, NODE_NAME);

        List<Future<byte[]>> traceDataRequests = requestClientsForTraceData(tracePath);
        return findLatestTrace(traceDataRequests, topicName, subscriptionName);
    }

    private List<Future<byte[]>> requestClientsForTraceData(String tracePath) {
        return clientManager.getClients()
                .stream()
                .map(client -> requestTrace(tracePath, client))
                .collect(Collectors.toList());
    }

    private Future<byte[]> requestTrace(String undeliveredPath, ZookeeperClient client) {
        return queryExecutor.submit(() -> client.getData(undeliveredPath));
    }

    private Optional<SentMessageTrace> findLatestTrace(List<Future<byte[]>> requests, TopicName topicName,
                                                       String subscriptionName) {
        return requests.stream().map(request -> unmarshall(request, topicName, subscriptionName))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.comparing(SentMessageTrace::getTimestamp));
    }

    private Optional<SentMessageTrace> unmarshall(Future<byte[]> future, TopicName topicName,
                                                  String subscriptionName) {
        try {
            byte[] data = future.get();
            return Optional.of(mapper.readValue(data, SentMessageTrace.class));
        } catch (Exception e) {
            logger.warn(format("Could not read latest undelivered message for topic: %s and " +
                            "subscription: %s .", topicName.qualifiedName(), subscriptionName),
                    e);
            return Optional.empty();
        }
    }
}
