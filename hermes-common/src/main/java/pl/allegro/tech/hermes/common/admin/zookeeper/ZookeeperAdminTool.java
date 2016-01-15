package pl.allegro.tech.hermes.common.admin.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.Reaper;
import org.apache.zookeeper.CreateMode;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.admin.AdminTool;
import pl.allegro.tech.hermes.common.admin.AdminToolStartupException;
import pl.allegro.tech.hermes.common.exception.RetransmissionException;
import pl.allegro.tech.hermes.common.exception.SubscriptionEndpointAddressChangeException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import static pl.allegro.tech.hermes.common.admin.AdminTool.Operations.RETRANSMIT;
import static pl.allegro.tech.hermes.common.admin.AdminTool.Operations.RESTART_CONSUMER;

public class ZookeeperAdminTool implements AdminTool {

    private final ZookeeperPaths zookeeperPaths;
    private final CuratorFramework curatorFramework;
    private final ObjectMapper objectMapper;
    private final Reaper reaper;

    public ZookeeperAdminTool(ZookeeperPaths zookeeperPaths, CuratorFramework curatorFramework,
                              ObjectMapper objectMapper, int reapingInterval) {
        this.zookeeperPaths = zookeeperPaths;
        this.curatorFramework = curatorFramework;
        this.objectMapper = objectMapper;
        this.reaper = new Reaper(curatorFramework, reapingInterval);
    }

    public void start() throws AdminToolStartupException {
        try {
            this.reaper.start();
        } catch (Exception ex) {
            throw new AdminToolStartupException(ex);
        }
    }

    @Override
    public void retransmit(SubscriptionName subscriptionName) {
        try {
            executeAdminOperation(subscriptionName, RETRANSMIT.name());
        } catch (Exception e) {
            throw new RetransmissionException(e);
        }
    }

    @Override
    public void restartConsumer(SubscriptionName subscriptionName) {
        try {
            executeAdminOperation(subscriptionName, RESTART_CONSUMER.name());
        } catch (Exception e) {
            throw new SubscriptionEndpointAddressChangeException(e);
        }
    }

    private void executeAdminOperation(SubscriptionName subscriptionName, String name) throws Exception {
        String path = zookeeperPaths.adminOperationPath(name);

        String createdPath = curatorFramework.create()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(path, objectMapper.writeValueAsBytes(subscriptionName));

        reaper.addPath(createdPath);
    }
}
