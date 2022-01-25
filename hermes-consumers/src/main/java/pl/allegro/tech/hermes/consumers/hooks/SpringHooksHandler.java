package pl.allegro.tech.hermes.consumers.hooks;

import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

public class SpringHooksHandler {

    private final List<SpringServiceAwareHook> beforeStartHooks = new ArrayList<>();
    private final List<SpringServiceAwareHook> startupHooks = new ArrayList<>();
    private final List<SpringServiceAwareHook> shutdownHooks = new ArrayList<>();
    private boolean disabledGlobalShutdownHook = false;

    public void addBeforeStartHook(SpringServiceAwareHook hook) {
        beforeStartHooks.add(hook);
    }

    public void addStartupHook(SpringServiceAwareHook hook) {
        startupHooks.add(hook);
    }

    public void addShutdownHook(SpringServiceAwareHook hook) {
        shutdownHooks.add(hook);
    }

    public void runBeforeStartHooks(ApplicationContext context) {
        runHooksInOrder(beforeStartHooks, context);
    }

    public void shutdown(ApplicationContext context) {
        runShutdownHooks(context);
    }

    public void startup(ApplicationContext context) {
        if (!disabledGlobalShutdownHook) {
            registerGlobalShutdownHook(context);
        }
        runHooksInOrder(startupHooks, context);
    }

    public void disableGlobalShutdownHook() {
        disabledGlobalShutdownHook = true;
    }

    private void runShutdownHooks(ApplicationContext context) {
        runHooksInOrder(shutdownHooks, context);
    }

    private void runHooksInOrder(List<SpringServiceAwareHook> hooks, ApplicationContext context) {
        hooks.stream()
                .sorted((h1, h2) -> h2.getPriority() - h1.getPriority())
                .forEach(c -> c.accept(context));
    }

    private void registerGlobalShutdownHook(ApplicationContext context) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                setName("GlobalShutdownHook");
                runShutdownHooks(context);
            }
        });
    }
}
