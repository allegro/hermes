package pl.allegro.tech.hermes.management.infrastructure.metrics

import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.DistributedEphemeralCounter
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager
import pl.allegro.tech.hermes.management.utils.MultiZookeeperIntegrationTest

class SummedDistributedEphemeralCounterTest extends MultiZookeeperIntegrationTest {

    ZookeeperClientManager manager

    DistributedEphemeralCounter distributedEphemeralCounterDc1
    DistributedEphemeralCounter distributedEphemeralCounterDc2
    SummedDistributedEphemeralCounter summedDistributedEphemeralCounter

    def setup() {
        manager = buildZookeeperClientManager()
        manager.start()
        assertZookeeperClientsConnected(manager.clients)

        def zkClientDc1 = findClientByDc(manager.clients, DC_1_NAME).curatorFramework
        distributedEphemeralCounterDc1 = new DistributedEphemeralCounter(zkClientDc1)

        def zkClientDc2 = findClientByDc(manager.clients, DC_2_NAME).curatorFramework
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
}
