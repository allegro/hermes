package pl.allegro.tech.hermes.common.hook;

import org.glassfish.hk2.api.ServiceLocator;

import java.util.ArrayList;
import java.util.List;

public class HooksHandler {
    private final List<Hook.Startup> startupHooks = new ArrayList<>();
    private final List<Hook.Shutdown> shutdownHooks = new ArrayList<>();

    public void addStartupHook(Hook.Startup hook) {
        startupHooks.add(hook);
    }

    public void addShutdownHook(Hook.Shutdown hook) {
        shutdownHooks.add(hook);
    }

    public void shutdown() {
        shutdownHooks.forEach(Hook.Shutdown::apply);
    }

    public void startup(ServiceLocator serviceLocator) {
        registerGlobalShutdownHook();
        startupHooks.forEach(hook -> hook.apply(serviceLocator));
    }

    private void registerGlobalShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown();
            }
        });
    }
}
