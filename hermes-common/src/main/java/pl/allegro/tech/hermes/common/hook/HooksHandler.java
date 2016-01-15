package pl.allegro.tech.hermes.common.hook;

import org.glassfish.hk2.api.ServiceLocator;

import java.util.ArrayList;
import java.util.List;

public class HooksHandler {

    private final List<ServiceAwareHook> startupHooks = new ArrayList<>();
    private final List<ServiceAwareHook> shutdownHooks = new ArrayList<>();

    public void addStartupHook(ServiceAwareHook hook) {
        startupHooks.add(hook);
    }

    public void addShutdownHook(ServiceAwareHook hook) {
        shutdownHooks.add(hook);
    }

    public void shutdown(ServiceLocator serviceLocator) {
        shutdownHooks.forEach(c -> c.accept(serviceLocator));
    }

    public void startup(ServiceLocator serviceLocator) {
        registerGlobalShutdownHook(serviceLocator);
        startupHooks.forEach(c -> c.accept(serviceLocator));
    }

    private void registerGlobalShutdownHook(ServiceLocator serviceLocator) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown(serviceLocator);
            }
        });
    }
}
