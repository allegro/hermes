package pl.allegro.tech.hermes.frontend.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicMetadataLoadingStartupHook {
    private static final Logger logger = LoggerFactory.getLogger(TopicMetadataLoadingStartupHook.class);

    private final TopicMetadataLoadingRunner topicMetadataLoadingRunner;

    private final boolean isTopicMetadataLoadingStartupHookEnabled;

    public TopicMetadataLoadingStartupHook(TopicMetadataLoadingRunner topicMetadataLoadingRunner,
                                           boolean isTopicMetadataLoadingStartupHookEnabled) {
        this.topicMetadataLoadingRunner = topicMetadataLoadingRunner;
        this.isTopicMetadataLoadingStartupHookEnabled = isTopicMetadataLoadingStartupHookEnabled;
    }

    public void run() {
        if (isTopicMetadataLoadingStartupHookEnabled) {
            try {
                topicMetadataLoadingRunner.refreshMetadata();
            } catch (Exception e) {
                logger.error("An error occurred while refreshing topic metadata", e);
            }
        } else {
            logger.info("Topic metadata loading startup hook is disabled");
        }
    }
}
