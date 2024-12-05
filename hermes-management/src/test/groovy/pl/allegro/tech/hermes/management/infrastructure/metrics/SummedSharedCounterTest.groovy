package pl.allegro.tech.hermes.management.infrastructure.metrics

import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager
import pl.allegro.tech.hermes.management.utils.MultiZookeeperIntegrationTest

import java.time.Duration

class SummedSharedCounterTest extends MultiZookeeperIntegrationTest {

    static final EXPIRE_AFTER = Duration.ofHours(72)
    static final DISTRIBUTED_LEADER_BACKOFF = Duration.ofSeconds(1)
    static final DISTRIBUTED_LEADER_RETRIES = 3
    static final COUNTER_PATH = '/hermes/shared/counter'

    ZookeeperClientManager manager

    SharedCounter sharedCounterDc1
    SharedCounter sharedCounterDc2
    SummedSharedCounter summedSharedCounter

    def setup() {
        manager = buildZookeeperClientManager()
        manager.start()
        assertZookeeperClientsConnected(manager.clients)

        def zkClientDc1 = findClientByDc(manager.clients, DC_1_NAME).curatorFramework
        sharedCounterDc1 = new SharedCounter(zkClientDc1, EXPIRE_AFTER, DISTRIBUTED_LEADER_BACKOFF, DISTRIBUTED_LEADER_RETRIES)

        def zkClientDc2 = findClientByDc(manager.clients, DC_2_NAME).curatorFramework
        sharedCounterDc2 = new SharedCounter(zkClientDc2, EXPIRE_AFTER, DISTRIBUTED_LEADER_BACKOFF, DISTRIBUTED_LEADER_RETRIES)

        summedSharedCounter = new SummedSharedCounter(manager.clients, (int) EXPIRE_AFTER.toHours(), (int) DISTRIBUTED_LEADER_BACKOFF.toMillis(), DISTRIBUTED_LEADER_RETRIES)
    }

    def cleanup() {
        manager.stop()
    }

    def "should return sum of shared counters"() {
        given:
        sharedCounterDc1.increment(COUNTER_PATH, 1)
        sharedCounterDc2.increment(COUNTER_PATH, 1)

        expect:
        summedSharedCounter.getValue(COUNTER_PATH) == 2
    }

    def "should return last modified time of shared counters"() {
        when:
        sharedCounterDc1.increment(COUNTER_PATH, 1)
        def sharedCounterDc1Mtime = getMtime(DC_1_NAME, COUNTER_PATH)

        then:
        summedSharedCounter.getLastModified(COUNTER_PATH).get().toEpochMilli() == sharedCounterDc1Mtime

        when:
        sharedCounterDc2.increment(COUNTER_PATH, 1)
        def sharedCounterDc2Mtime = getMtime(DC_2_NAME, COUNTER_PATH)

        then:
        summedSharedCounter.getLastModified(COUNTER_PATH).get().toEpochMilli() == sharedCounterDc2Mtime
    }

    def "should return empty optional for last modified time of non existing counter"() {
        expect:
        summedSharedCounter.getLastModified("/does/not/exist") == Optional.empty()
    }

    private def getMtime(String dc, String counterPath) {
        return findClientByDc(manager.clients, dc)
                .curatorFramework
                .checkExists()
                .forPath(counterPath)
                .mtime
    }
}
