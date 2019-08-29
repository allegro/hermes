package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.domain.workload.constraints.ConsumersWorkloadConstraints;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang.ArrayUtils.isNotEmpty;

public class ZookeeperWorkloadConstraintsRepository extends ZookeeperBasedRepository implements WorkloadConstraintsRepository {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperWorkloadConstraintsRepository.class);

    private final ZookeeperWorkloadConstraintsPathChildrenCache pathChildrenCache;

    public ZookeeperWorkloadConstraintsRepository(CuratorFramework curator, ObjectMapper mapper, ZookeeperPaths paths) {
        this(curator, mapper, paths,
                new ZookeeperWorkloadConstraintsPathChildrenCache(curator, paths.consumersWorkloadConstraintsPath()));
    }

    ZookeeperWorkloadConstraintsRepository(CuratorFramework curator, ObjectMapper mapper, ZookeeperPaths paths,
                                           ZookeeperWorkloadConstraintsPathChildrenCache pathChildrenCache) {
        super(curator, mapper, paths);
        this.pathChildrenCache = pathChildrenCache;
        try {
            this.pathChildrenCache.start();
        } catch (Exception e) {
            throw new InternalProcessingException("ZookeeperWorkloadConstraintsPathChildrenCache cannot start.", e);
        }
    }

    @Override
    public ConsumersWorkloadConstraints getConsumersWorkloadConstraints() {
        try {
            final Map<TopicName, Constraints> topicConstraints = new HashMap<>();
            final Map<SubscriptionName, Constraints> subscriptionConstraints = new HashMap<>();
            pathChildrenCache.getChildrenData()
                    .forEach(childData -> {
                        String childNode = paths.extractChildNode(childData.getPath(), paths.consumersWorkloadConstraintsPath());
                        try {
                            final byte[] data = childData.getData();
                            if (isNotEmpty(data)) {
                                final Constraints constraints = mapper.readValue(data, Constraints.class);
                                if (isSubscription(childNode)) {
                                    subscriptionConstraints.put(SubscriptionName.fromString(childNode), constraints);
                                } else {
                                    topicConstraints.put(TopicName.fromQualifiedName(childNode), constraints);
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Error while reading data from node {}", childData.getPath(), e);
                        }
                    });
            return new ConsumersWorkloadConstraints(topicConstraints, subscriptionConstraints);
        } catch (Exception e) {
            logger.warn("Error while reading path {}", paths.consumersWorkloadConstraintsPath(), e);
            return new ConsumersWorkloadConstraints(emptyMap(), emptyMap());
        }
    }

    private boolean isSubscription(String path) {
        return path.contains("$");
    }
}
