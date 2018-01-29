package pl.allegro.tech.hermes.management.config.storage;

import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperAuthProperties;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClusterProperties;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperProperties;

import java.util.stream.Collectors;

class ZookeeperPropertiesMapper {
    public ZookeeperProperties fromStorageClustersProperies(StorageClustersProperties source) {
        ZookeeperProperties target = new ZookeeperProperties();
        target.setPathPrefix(source.getPathPrefix());
        target.setRetryTimes(source.getRetryTimes());
        target.setRetrySleep(source.getRetrySleep());
        target.setTransactional(source.isTransactional());
        target.setAuthorization(mapStorageAuthorizationProperties(source.getAuthorization()));
        target.setClusters(
                source.getClusters()
                        .stream()
                        .map(this::mapStorageProperties)
                        .collect(Collectors.toList())
        );
        return target;
    }

    private ZookeeperClusterProperties mapStorageProperties(StorageProperties source) {
        ZookeeperClusterProperties target = new ZookeeperClusterProperties();
        target.setName(source.getName());
        target.setDc(source.getDc());
        target.setConnectionString(source.getConnectionString());
        target.setConnectTimeout(source.getConnectTimeout());
        target.setSessionTimeout(source.getSessionTimeout());
        return target;
    }

    private ZookeeperAuthProperties mapStorageAuthorizationProperties(StorageAuthorizationProperties source) {
        ZookeeperAuthProperties target = new ZookeeperAuthProperties();
        target.setUser(source.getUser());
        target.setPassword(source.getPassword());
        target.setScheme(source.getScheme());
        return target;
    }
}
