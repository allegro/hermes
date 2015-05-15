package pl.allegro.tech.hermes.integration.helper;

import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import static org.assertj.core.api.Assertions.assertThat;

public class Assertions {

    private final CuratorFramework zookeeper;
    private final ZookeeperPaths zookeeperPaths = new ZookeeperPaths(Configs.ZOOKEEPER_ROOT.getDefaultValue().toString());

    public Assertions(CuratorFramework zookeeper) {
        this.zookeeper = zookeeper;
    }

    public void topicDetailsNotExists(TopicName topicName) throws Exception {
        assertThat(zookeeper.checkExists().forPath(zookeeperPaths.topicPath(topicName))).isNull();
    }
}
