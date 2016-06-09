package pl.allegro.tech.hermes.common.hook;

import ch.qos.logback.classic.LoggerContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HooksHandler {

    private final List<ServiceAwareHook> startupHooks = new ArrayList<>();
    private final List<ServiceAwareHook> shutdownHooks = new ArrayList<>();
    private boolean disabledGlobalShutdownHook = false;

    public void addStartupHook(ServiceAwareHook hook) {
        startupHooks.add(hook);
    }

    public void addShutdownHook(ServiceAwareHook hook) {
        shutdownHooks.add(hook);
    }

    public void shutdown(ServiceLocator serviceLocator) {
        runShutdownHooks(serviceLocator);
    }

    public void startup(ServiceLocator serviceLocator) {
        if (!disabledGlobalShutdownHook) {
            registerGlobalShutdownHook(serviceLocator);
        }
        startupHooks.forEach(c -> c.accept(serviceLocator));
    }

    public void disableGlobalShutdownHook() {
        disabledGlobalShutdownHook = true;
    }

    private void runShutdownHooks(ServiceLocator serviceLocator) {
        try {
            shutdownHooks.forEach(c -> c.accept(serviceLocator));
        } finally {
            ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
        }
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
