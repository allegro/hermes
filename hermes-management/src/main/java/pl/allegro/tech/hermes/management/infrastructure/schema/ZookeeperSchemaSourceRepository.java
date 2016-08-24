package pl.allegro.tech.hermes.management.infrastructure.schema;

import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.ZookeeperSchemaSourceProvider;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.topic.schema.CouldNotDeleteSchemaException;
import pl.allegro.tech.hermes.management.domain.topic.schema.CouldNotSaveSchemaException;
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaSourceRepository;

public class ZookeeperSchemaSourceRepository extends ZookeeperSchemaSourceProvider implements SchemaSourceRepository {

    public ZookeeperSchemaSourceRepository(CuratorFramework curatorFramework, ZookeeperPaths zkPaths) {
        super(curatorFramework, zkPaths);
    }

    @Override
    public void save(SchemaSource schemaSource, Topic topic) {
        try {
            String schemaPath = zkPaths.topicPath(topic.getName(), SCHEMA_SUFFIX);
            byte[] schema = schemaSource.value().getBytes();
            if (curatorFramework.checkExists().forPath(schemaPath) == null) {
                curatorFramework.create().creatingParentsIfNeeded().forPath(schemaPath, schema);
            } else {
                curatorFramework.setData().forPath(schemaPath, schema);
            }
        } catch (Exception e) {
            throw new CouldNotSaveSchemaException("Could not store in zookeeper schema for topic " + topic.getQualifiedName(), e);
        }
    }

    @Override
    public void delete(Topic topic) {
        try {
            curatorFramework.delete().forPath(zkPaths.topicPath(topic.getName(), SCHEMA_SUFFIX));
        } catch (Exception e) {
            throw new CouldNotDeleteSchemaException("Could not delete from zookeeper schema for topic " + topic.getQualifiedName(), e);
        }
    }
}
