package pl.allegro.tech.hermes.common.hook;

import java.util.ArrayList;
import java.util.List;

public class HooksHandler {
    private final List<Hook> startupHooks = new ArrayList<>();
    private final List<Hook> shutdownHooks = new ArrayList<>();

    public void addStartupHook(Hook hook) {
        startupHooks.add(hook);
    }

    public void addShutdownHook(Hook hook) {
        shutdownHooks.add(hook);
    }

    public void shutdown() {
        shutdownHooks.forEach(Hook::apply);
    }

    public void startup() {
        registerGlobalShutdownHook();
        startupHooks.forEach(Hook::apply);
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
