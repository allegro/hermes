package pl.allegro.tech.hermes.frontend.server;

import org.glassfish.hk2.api.ServiceLocator;
import pl.allegro.tech.hermes.common.hook.Hook;
import pl.allegro.tech.hermes.common.hook.ServiceAwareHook;

import javax.inject.Inject;

public class TopicMetadataLoadingStartupHook implements ServiceAwareHook {

    private final TopicMetadataLoadingRunner topicMetadataLoadingRunner;

    @Inject
    public TopicMetadataLoadingStartupHook(TopicMetadataLoadingRunner topicMetadataLoadingRunner) {
        this.topicMetadataLoadingRunner = topicMetadataLoadingRunner;
    }

    @Override
    public void accept(ServiceLocator serviceLocator) {
        topicMetadataLoadingRunner.refreshMetadata();
    }

    @Override
    public int getPriority() {
        return Hook.HIGHER_PRIORITY;
    }

}
