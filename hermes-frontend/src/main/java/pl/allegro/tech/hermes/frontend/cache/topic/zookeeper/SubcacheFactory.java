package pl.allegro.tech.hermes.frontend.cache.topic.zookeeper;

import org.apache.curator.framework.CuratorFramework;

import java.util.concurrent.ExecutorService;

interface SubcacheFactory {

    HierarchicalCache create(CuratorFramework curator, ExecutorService executorService, String path);

}
