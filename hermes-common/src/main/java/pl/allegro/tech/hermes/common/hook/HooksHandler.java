package pl.allegro.tech.hermes.common.hook;

import org.glassfish.hk2.api.ServiceLocator;

import java.util.ArrayList;
import java.util.List;

public class HooksHandler {

    private final List<ServiceAwareHook> beforeStartHooks = new ArrayList<>();
    private final List<ServiceAwareHook> startupHooks = new ArrayList<>();
    private final List<ServiceAwareHook> shutdownHooks = new ArrayList<>();
    private boolean disabledGlobalShutdownHook = false;

    public void addBeforeStartHook(ServiceAwareHook hook) {
        beforeStartHooks.add(hook);
    }

    public void addStartupHook(ServiceAwareHook hook) {
        startupHooks.add(hook);
    }

    public void addShutdownHook(ServiceAwareHook hook) {
        shutdownHooks.add(hook);
    }

    public void runBeforeStartHooks(ServiceLocator serviceLocator) {
        runHooksInOrder(beforeStartHooks, serviceLocator);
    }

    public void shutdown(ServiceLocator serviceLocator) {
        runShutdownHooks(serviceLocator);
    }

    public void startup(ServiceLocator serviceLocator) {
        if (!disabledGlobalShutdownHook) {
            registerGlobalShutdownHook(serviceLocator);
        }
        runHooksInOrder(startupHooks, serviceLocator);
    }

    public void disableGlobalShutdownHook() {
        disabledGlobalShutdownHook = true;
    }

    private void runShutdownHooks(ServiceLocator serviceLocator) {
        runHooksInOrder(shutdownHooks, serviceLocator);
    }

    private void runHooksInOrder(List<ServiceAwareHook> hooks, ServiceLocator serviceLocator) {
        hooks.stream()
                .sorted((h1, h2) -> h2.getPriority() - h1.getPriority())
                .forEach(c -> c.accept(serviceLocator));
    }

    private void registerGlobalShutdownHook(ServiceLocator serviceLocator) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                setName("GlobalShutdownHook");
                runShutdownHooks(serviceLocator);
            }
        });
    }
}
