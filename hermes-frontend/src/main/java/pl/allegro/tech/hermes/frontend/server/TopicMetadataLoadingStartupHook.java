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
            topicMetadataLoadingRunner.refreshMetadataForLocalDatacenter();
        } else {
            logger.info("Topic metadata loading startup hook is disabled");
        }
    }
}
