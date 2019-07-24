package pl.allegro.tech.hermes.management.infrastructure.metrics

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.api.GetChildrenBuilder
import org.apache.curator.framework.api.GetDataBuilder
import org.apache.zookeeper.KeeperException
import org.junit.Rule
import org.springframework.boot.test.rule.OutputCapture
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.DistributedEphemeralCounter
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager
import pl.allegro.tech.hermes.management.utils.MultiZookeeperIntegrationTest

import java.nio.ByteBuffer

class SummedDistributedEphemeralCounterTest extends MultiZookeeperIntegrationTest {

    @Rule
    OutputCapture output = new OutputCapture()

    CuratorFramework zkClientDc1
    CuratorFramework zkClientDc2
    ZookeeperClientManager manager

    DistributedEphemeralCounter distributedEphemeralCounterDc1
    DistributedEphemeralCounter distributedEphemeralCounterDc2
    SummedDistributedEphemeralCounter summedDistributedEphemeralCounter

    def setup() {
        manager = buildZookeeperClientManager()
        manager.start()
        assertZookeeperClientsConnected(manager.clients)

        zkClientDc1 = findClientByDc(manager.clients, DC_1_NAME).curatorFramework
        distributedEphemeralCounterDc1 = new DistributedEphemeralCounter(zkClientDc1)

        zkClientDc2 = findClientByDc(manager.clients, DC_2_NAME).curatorFramework
        distributedEphemeralCounterDc2 = new DistributedEphemeralCounter(zkClientDc2)

        def zkClients = manager.clients.collect { client -> client.curatorFramework }
        summedDistributedEphemeralCounter = new SummedDistributedEphemeralCounter(zkClients)
    }

    def cleanup() {
        manager.stop()
    }

    def "should return sum of all node children values"() {
        given:
        distributedEphemeralCounterDc1.increment('/sum/host1/metrics', 1)
        distributedEphemeralCounterDc1.increment('/sum/host2/metrics', 1)
        distributedEphemeralCounterDc2.increment('/sum/host3/metrics', 1)
        distributedEphemeralCounterDc2.increment('/sum/host4/metrics', 1)

        expect:
        summedDistributedEphemeralCounter.getValue('/sum', '/metrics') == 4
    }

    def "should return partial sum if any node does not exist"() {
        given:
        distributedEphemeralCounterDc1.increment('/sum/host1/metrics', 1)
        distributedEphemeralCounterDc1.increment('/sum/host2/metrics', 1)

        expect:
        summedDistributedEphemeralCounter.getValue('/sum', '/metrics') == 2
        output.toString().contains "Error while reading value for base path: /sum and child path: /metrics; KeeperException.NoNodeException: KeeperErrorCode = NoNode for /sum"
    }

    def "should return partial sum if any node was removed during iteration"() {
        given:
        distributedEphemeralCounterDc2.increment('/sum/host3/metrics', 1)
        distributedEphemeralCounterDc2.increment('/sum/host4/metrics', 1)

        def zkFailingClientDc1 = Mock(CuratorFramework)
        def summedDistributedEphemeralCounter = new SummedDistributedEphemeralCounter([zkFailingClientDc1, zkClientDc2])

        def zkNodeChildren = Mock(GetChildrenBuilder)
        def zkData = Mock(GetDataBuilder)

        when:
        def result = summedDistributedEphemeralCounter.getValue('/sum', '/metrics')

        then:
        1 * zkFailingClientDc1.getChildren() >> zkNodeChildren
        1 * zkNodeChildren.forPath('/sum') >> ['host1', 'host2']

        2 * zkFailingClientDc1.getData() >> zkData
        1 * zkData.forPath('/sum/host1/metrics') >> longToBytes(1L)
        1 * zkData.forPath('/sum/host2/metrics') >> { throw new KeeperException.NoNodeException() }

        and:
        result == 3
    }

    byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES)
        buffer.putLong(x)
        buffer.array()
    }
}
