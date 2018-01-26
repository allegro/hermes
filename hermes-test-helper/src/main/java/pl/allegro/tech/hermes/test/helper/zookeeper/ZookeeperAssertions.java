package pl.allegro.tech.hermes.test.helper.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

public class ZookeeperAssertions {

    private CuratorFramework client;
    private ObjectMapper mapper;

    public ZookeeperAssertions(CuratorFramework client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public boolean zookeeperPathContains(String path, Object expectedContent) {
        try {
            byte[] data = client.getData().forPath(path);
            Object actualContent = mapper.readValue(data, expectedContent.getClass());
            return expectedContent.equals(actualContent);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }
}
