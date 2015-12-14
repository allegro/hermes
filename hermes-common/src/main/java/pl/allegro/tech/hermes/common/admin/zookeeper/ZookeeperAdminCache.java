package pl.allegro.tech.hermes.common.admin.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.admin.AdminOperationsCallback;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

import static pl.allegro.tech.hermes.common.admin.AdminTool.Operations.RETRANSMIT;

public class ZookeeperAdminCache extends PathChildrenCache implements PathChildrenCacheListener {

    private final ObjectMapper objectMapper;
    private final List<AdminOperationsCallback> adminCallbacks = new ArrayList<>();

    @Inject
    public ZookeeperAdminCache(ZookeeperPaths zookeeperPaths, @Named(CuratorType.HERMES) CuratorFramework client, ObjectMapper objectMapper) {
        super(client, zookeeperPaths.adminPath(), true);
        this.objectMapper = objectMapper;

        getListenable().addListener(this);
    }

    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        switch (event.getType()) {
            case CHILD_ADDED:
                if (event.getData().getPath().contains(RETRANSMIT.name())) {
                    SubscriptionName subscriptionName = objectMapper.readValue(event.getData().getData(), SubscriptionName.class);

                    for (AdminOperationsCallback adminCallback : adminCallbacks) {
                        adminCallback.onRetransmissionStarts(subscriptionName);
                    }
                    client.delete().forPath(event.getData().getPath());
                }
                break;
            default:
                break;
        }
    }

    public void addCallback(AdminOperationsCallback callback) {
        adminCallbacks.add(callback);
    }
}
