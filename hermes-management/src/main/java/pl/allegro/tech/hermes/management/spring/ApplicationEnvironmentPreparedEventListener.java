package pl.allegro.tech.hermes.management.spring;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;


public abstract class ApplicationEnvironmentPreparedEventListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>{

    protected abstract void onApplicationEnvironmentPreparedEvent(ApplicationEnvironmentPreparedEvent event);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        // don't listen to events in a bootstrap context
        if (event.getEnvironment().getPropertySources().contains("bootstrapInProgress")) {
            return;
        }

        onApplicationEnvironmentPreparedEvent(event);
    }
}