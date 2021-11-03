package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Readiness;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.readiness.ReadinessRepository;

public class ZookeeperDatacenterReadinessRepository extends ZookeeperBasedRepository implements ReadinessRepository, NodeCacheListener {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperDatacenterReadinessRepository.class);

    private final NodeCache cache;
    private final ObjectMapper mapper;

    private volatile boolean ready = true;

    public ZookeeperDatacenterReadinessRepository(CuratorFramework curator, ObjectMapper mapper, ZookeeperPaths paths) {
        super(curator, mapper, paths);
        this.mapper = mapper;
        this.cache = new NodeCache(curator, paths.frontendReadinessPath());
        cache.getListenable().addListener(this);
        try {
            cache.start(true);
        } catch (Exception e) {
            throw new InternalProcessingException("Readiness cache cannot start.", e);
        }
        refreshReadiness();
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void setReadiness(boolean isReady) {
        try {
            String path = paths.frontendReadinessPath();
            if (!pathExists(path)) {
                zookeeper.create()
                        .creatingParentsIfNeeded()
                        .forPath(path, mapper.writeValueAsBytes(new Readiness(isReady)));
            } else {
                zookeeper.setData().forPath(path, mapper.writeValueAsBytes(new Readiness(isReady)));
            }
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }

    @Override
    public void close() {
        try {
            cache.close();
        } catch (Exception e) {
            logger.warn("Failed to stop readiness cache", e);
        }
    }

    @Override
    public void nodeChanged() {
        refreshReadiness();
    }

    private void refreshReadiness() {
        try {
            ChildData nodeData = cache.getCurrentData();
            if (nodeData != null) {
                byte[] data = nodeData.getData();
                Readiness readiness = mapper.readValue(data, Readiness.class);
                ready = readiness.isReady();
            } else {
                ready = true;
            }
        } catch (Exception e) {
            logger.error("Failed reloading readiness cache. Current value: ready=" + ready, e);
        }
    }
}
