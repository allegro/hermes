package pl.allegro.tech.hermes.domain.topic.schema;

import com.googlecode.catchexception.CatchException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import java.util.Optional;

import static com.googlecode.catchexception.CatchException.catchException;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;

public class ZookeeperSchemaSourceProviderTest extends ZookeeperBaseTest {

    private static final String AVRO_SCHEMA = "{type:\"record\",name:\"schema\",namespace:\"com.avro\",fields:[{name:\"username\",type:\"string\",doc:\"Name of the user account\"}],\"doc:\":\"A basic schema\"}";

    private ZookeeperSchemaSourceProvider provider = new ZookeeperSchemaSourceProvider(zookeeperClient, new ZookeeperPaths("/test"));

    @Test
    public void shouldGetTopicSchemaFromZookeeper() throws Exception {
        // given
        Topic topic = topic().withName("org.hermes.schema", "existing").build();
        zookeeperClient.create().creatingParentsIfNeeded().forPath("/test/groups/org.hermes.schema/topics/existing/schema", AVRO_SCHEMA.getBytes());

        // when
        Optional<SchemaSource> schemaSource = provider.get(topic);

        // then
        assertThat(schemaSource).isPresent().contains(SchemaSource.valueOf(AVRO_SCHEMA));
    }

    @Test
    public void shouldReturnEmptyWhenSchemaNotFound() {
        // given
        Topic topic = topic().withName("org.hermes.schema", "notExisting").build();

        // when
        Optional<SchemaSource> schemaSource = provider.get(topic);

        // then
        assertThat(schemaSource).isEmpty();
    }

    @Test
    public void shouldThrowExceptionWhenCouldNotConnectToZK() {
        // given
        Topic topic = topic().withName("org.hermes.schema", "broken").build();
        CuratorFramework notStartedClient = CuratorFrameworkFactory.newClient(zookeeperServer.getConnectString(), new ExponentialBackoffRetry(100, 1));
        ZookeeperSchemaSourceProvider brokenProvider = new ZookeeperSchemaSourceProvider(notStartedClient, new ZookeeperPaths("/test"));

        // when
        catchException(brokenProvider).get(topic);

        // then
        assertThat(CatchException.<Exception>caughtException()).isInstanceOf(InternalProcessingException.class);
    }

}
