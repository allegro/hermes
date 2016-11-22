package pl.allegro.tech.hermes.common.hook;

import org.glassfish.hk2.api.ServiceLocator;

import java.util.function.Consumer;

public interface ServiceAwareHook extends Consumer<ServiceLocator> {

    /*
        Hooks with higher priority are executed in the first place
     */
    default int getPriority() {
        return Hook.NORMAL_PRIORITY;
    }
}
