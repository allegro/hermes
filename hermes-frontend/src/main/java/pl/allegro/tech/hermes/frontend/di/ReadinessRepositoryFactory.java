package pl.allegro.tech.hermes.frontend.di;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.domain.readiness.ReadinessRepository;
import pl.allegro.tech.hermes.frontend.server.KafkaHealthChecker;
import pl.allegro.tech.hermes.frontend.server.KafkaReadOnlyReadinessRepository;
import pl.allegro.tech.hermes.frontend.server.TopicMetadataLoadingRunner;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ReadOnlyCachedDatacenterReadinessRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperDatacenterReadinessRepository;

import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_READINESS_CHECK_ENABLED;


public class ReadinessRepositoryFactory implements Factory<List<ReadinessRepository>> {

    private final CuratorFramework zookeeper;

    private final ZookeeperPaths paths;

    private final ObjectMapper mapper;

    private final ConfigFactory config;
    private final TopicMetadataLoadingRunner topicMetadataLoadingRunner;

    @Inject
    public ReadinessRepositoryFactory(@Named(CuratorType.HERMES) CuratorFramework zookeeper, ZookeeperPaths paths,
                                      ObjectMapper mapper, ConfigFactory config, TopicMetadataLoadingRunner topicMetadataLoadingRunner) {
        this.zookeeper = zookeeper;
        this.paths = paths;
        this.mapper = mapper;
        this.config = config;
        this.topicMetadataLoadingRunner = topicMetadataLoadingRunner;
    }

    @Override
    public List<ReadinessRepository> provide() {
        List<ReadinessRepository> repositories = new ArrayList<>();
        if (config.getBooleanProperty(FRONTEND_READINESS_CHECK_ENABLED)) {
            ZookeeperDatacenterReadinessRepository zookeeperDatacenterReadinessRepository = new ZookeeperDatacenterReadinessRepository(zookeeper, mapper, paths);
            repositories.add(new ReadOnlyCachedDatacenterReadinessRepository(zookeeperDatacenterReadinessRepository));
            repositories.add(new KafkaReadOnlyReadinessRepository(new KafkaHealthChecker(topicMetadataLoadingRunner, config)));
        }

        return Lists.newArrayList(repositories);
    }

    @Override
    public void dispose(List<ReadinessRepository> instances) {
    }
}
