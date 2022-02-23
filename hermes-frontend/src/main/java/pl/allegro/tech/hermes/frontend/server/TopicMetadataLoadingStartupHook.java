package pl.allegro.tech.hermes.frontend.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class TopicMetadataLoadingStartupHook {
    private static final Logger logger = LoggerFactory.getLogger(TopicMetadataLoadingStartupHook.class);

    private final TopicMetadataLoadingRunner topicMetadataLoadingRunner;

    @Inject
    public TopicMetadataLoadingStartupHook(TopicMetadataLoadingRunner topicMetadataLoadingRunner) {
        this.topicMetadataLoadingRunner = topicMetadataLoadingRunner;
    }

    public void run() {
        try {
            topicMetadataLoadingRunner.refreshMetadata();
        } catch (Exception e) {
            logger.error("An error occurred while refreshing topic metadata", e);
        }
    }
}
