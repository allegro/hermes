package pl.allegro.tech.hermes.consumers.hooks;

import org.springframework.context.ApplicationContext;
import pl.allegro.tech.hermes.common.hook.Hook;

import java.util.function.Consumer;

public interface SpringServiceAwareHook extends Consumer<ApplicationContext> {

    /*
        Hooks with higher priority are executed in the first place
     */
    default int getPriority() {
        return Hook.NORMAL_PRIORITY;
    }
}
