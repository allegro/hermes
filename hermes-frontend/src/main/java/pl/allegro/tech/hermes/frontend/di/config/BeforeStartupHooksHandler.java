package pl.allegro.tech.hermes.frontend.di.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.List;

public class BeforeStartupHooksHandler {

    private final List<BeforeStartupHook> hooks;

    public BeforeStartupHooksHandler(List<BeforeStartupHook> hooks) {
        this.hooks = hooks;
    }

    private void runHooks() {
        hooks.stream()
                .sorted((h1, h2) -> h2.getPriority() - h1.getPriority())
                .forEach(Runnable::run);
    }
}
