package pl.allegro.tech.hermes.common.di.factories;

import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.DistributedEphemeralCounter;

import javax.inject.Inject;
import javax.inject.Named;

public class DistributedEphemeralCounterFactory implements Factory<DistributedEphemeralCounter> {

    private final CuratorFramework zookeeper;

    @Inject
    public DistributedEphemeralCounterFactory(@Named(CuratorType.HERMES) CuratorFramework zookeeper) {
        this.zookeeper = zookeeper;
    }

    @Override
    public DistributedEphemeralCounter provide() {
        return new DistributedEphemeralCounter(zookeeper);
    }

    @Override
    public void dispose(DistributedEphemeralCounter instance) {
    }
}
