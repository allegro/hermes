package pl.allegro.tech.hermes.infrastructure.zookeeper.counter;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Test;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class DistributedEphemeralCounterTest extends ZookeeperBaseTest {

    private final DistributedEphemeralCounter counter = new DistributedEphemeralCounter(zookeeperClient);

    @Test
    public void shouldIncrementAndRetrieveCounterValue() {
        // given when
        counter.increment("/increment/host/metric", 10);
        wait.untilZookeeperPathIsCreated("/increment/host/metric");

        // then
        assertThat(counter.getValue("/increment", "/metric")).isEqualTo(10);
        assertThat(counter.countOccurrences("/increment", "/metric")).isEqualTo(1);
    }

    @Test
    public void shouldReturnSumOfAllNodeChildrenValues() {
        // given when
        counter.increment("/sum/host1/metric", 10);
        counter.increment("/sum/host2/metric", 5);
        wait.untilZookeeperPathIsCreated("/sum/host1/metric");

        // then
        assertThat(counter.getValue("/sum", "/metric")).isEqualTo(15);
    }

    @Test
    public void shouldResetNodeWhenConnectionIsClosed() {
        // given
        try (CuratorFramework otherClient = otherClient()) {
            DistributedEphemeralCounter otherCounter = new DistributedEphemeralCounter(otherClient);

            otherCounter.increment("/ephemeral/host1/metric", 10);
            wait.untilZookeeperPathIsCreated("/ephemeral/host1/metric");
        }

        // when
        counter.increment("/ephemeral/host2/metric", 5);
        wait.untilZookeeperPathIsCreated("/ephemeral/host2/metric");

        // then
        assertThat(counter.getValue("/ephemeral", "/metric")).isEqualTo(5);
    }
}
