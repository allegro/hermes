package pl.allegro.tech.hermes.frontend.di.config;

import pl.allegro.tech.hermes.common.hook.Hook;

public interface BeforeStartupHook extends Runnable {

    /*
        Hooks with higher priority are executed in the first place
     */
    default int getPriority() {
        return Hook.NORMAL_PRIORITY;
    }
}
