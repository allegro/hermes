package pl.allegro.tech.hermes.infrastructure.zookeeper.client;

import java.util.Collections;
import java.util.List;

public class ZookeeperProperties {
    private String pathPrefix = "/hermes";
    private int retryTimes = 3;
    private int retrySleep = 1000;
    private int sharedCountersExpiration = 72;
    private int commandPoolSize = 100;
    private boolean transactional = true;
    private List<ZookeeperClusterProperties> clusters = Collections.emptyList();

    private ZookeeperAuthProperties authorization;

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public int getRetrySleep() {
        return retrySleep;
    }

    public void setRetrySleep(int retrySleep) {
        this.retrySleep = retrySleep;
    }

    public int getSharedCountersExpiration() {
        return sharedCountersExpiration;
    }

    public void setSharedCountersExpiration(int sharedCountersExpiration) {
        this.sharedCountersExpiration = sharedCountersExpiration;
    }

    public ZookeeperAuthProperties getAuthorization() {
        return authorization;
    }

    public void setAuthorization(ZookeeperAuthProperties authorization) {
        this.authorization = authorization;
    }

    public List<ZookeeperClusterProperties> getClusters() {
        return clusters;
    }

    public void setClusters(List<ZookeeperClusterProperties> clusters) {
        this.clusters = clusters;
    }

    public int getCommandPoolSize() {
        return commandPoolSize;
    }

    public void setCommandPoolSize(int commandPoolSize) {
        this.commandPoolSize = commandPoolSize;
    }

    public boolean isTransactional() {
        return transactional;
    }

    public void setTransactional(boolean transactional) {
        this.transactional = transactional;
    }
}
