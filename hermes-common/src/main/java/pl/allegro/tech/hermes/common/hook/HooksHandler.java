package pl.allegro.tech.hermes.common.hook;

import ch.qos.logback.classic.LoggerContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.slf4j.LoggerFactory;

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
//        try {
            shutdownHooks.forEach(c -> c.accept(serviceLocator));
//        }
//        finally {
//            ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
//        }
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
