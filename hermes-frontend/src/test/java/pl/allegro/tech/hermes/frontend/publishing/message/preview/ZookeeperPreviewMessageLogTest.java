package pl.allegro.tech.hermes.frontend.publishing.message.preview;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ZookeeperPreviewMessageLogTest extends ZookeeperBaseTest {

    private static final TopicName TOPIC = new TopicName("previewMessageLogGroup", "topic");

    private final ZookeeperPaths paths = new ZookeeperPaths("/hermes");

    private ZookeeperPreviewMessageLog log = new ZookeeperPreviewMessageLog(zookeeperClient, paths);

    @Before
    public void setUp() throws Exception {
        zookeeperClient.create().creatingParentsIfNeeded().forPath(paths.topicPath(TOPIC));
    }

    @After
    public void cleanUp() throws Exception {
        deleteData(paths.basePath());
    }

    @Test
    public void shouldAddPreviewMessageToLog() throws Exception {
        // given when
        log.add(new byte[1], TOPIC);
        log.persist();

        // then
        List<byte[]> lastMessage = log.last(TOPIC);
        assertThat(lastMessage.size()).isEqualTo(1);
    }

    @Test
    public void shouldHasInPreviewTwoMessagesAfterAdd3ToLog() throws Exception {
        // given when
        byte[] second = new byte[]{1, 2};
        byte[] third = new byte[]{2, 3, 4};
        log.add(new byte[]{0}, TOPIC);
        log.add(second, TOPIC);
        log.add(third, TOPIC);
        log.persist();

        // then
        List<byte[]> lastMessage = log.last(TOPIC);
        assertThat(lastMessage.size()).isEqualTo(2);
        assertThat(lastMessage.get(0)).isEqualTo(second);
        assertThat(lastMessage.get(1)).isEqualTo(third);
    }
}