package pl.allegro.tech.hermes.management.spring;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.MutablePropertySources;

public abstract class ApplicationEnvironmentPreparedEventListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    protected abstract void onApplicationEnvironmentPreparedEvent(ApplicationEnvironmentPreparedEvent event);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        // don't listen to events in a bootstrap context
        MutablePropertySources propertySources = event.getEnvironment().getPropertySources();
        if (propertySources.contains("bootstrapInProgress") || propertySources.contains("bootstrap")) {
            return;
        }

        onApplicationEnvironmentPreparedEvent(event);
    }
}
