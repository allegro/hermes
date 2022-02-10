package pl.allegro.tech.hermes.frontend.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.hook.Hook;
import pl.allegro.tech.hermes.frontend.di.config.BeforeStartupHook;

import javax.inject.Inject;

public class TopicMetadataLoadingStartupHook implements BeforeStartupHook { //ServiceAwareHook { //TODO: beforehook
    private static final Logger logger = LoggerFactory.getLogger(TopicMetadataLoadingStartupHook.class);

    private final TopicMetadataLoadingRunner topicMetadataLoadingRunner;

    @Inject
    public TopicMetadataLoadingStartupHook(TopicMetadataLoadingRunner topicMetadataLoadingRunner) {
        this.topicMetadataLoadingRunner = topicMetadataLoadingRunner;
    }

    @Override
//    public void accept(ServiceLocator serviceLocator) {
    public void run() {
        try {
            topicMetadataLoadingRunner.refreshMetadata();
        } catch (Exception e) {
            logger.error("An error occurred while refreshing topic metadata", e);
        }
    }

    @Override
    public int getPriority() {
        return Hook.NORMAL_PRIORITY;
    }

}
