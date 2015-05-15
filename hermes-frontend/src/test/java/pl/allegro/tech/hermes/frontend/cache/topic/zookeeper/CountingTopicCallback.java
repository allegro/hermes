package pl.allegro.tech.hermes.frontend.cache.topic.zookeeper;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicCallback;

import java.util.concurrent.CountDownLatch;

public class CountingTopicCallback implements TopicCallback {
    private final CountDownLatch createLatch = new CountDownLatch(1);
    private final CountDownLatch removeLatch = new CountDownLatch(1);
    private final CountDownLatch changeLatch = new CountDownLatch(1);

    @Override
    public void onTopicCreated(Topic topic) {
        createLatch.countDown();
    }

    @Override
    public void onTopicRemoved(Topic topic) {
        removeLatch.countDown();
    }

    @Override
    public void onTopicChanged(Topic topic) {
        changeLatch.countDown();
    }

    public CountDownLatch getCreateLatch() {
        return createLatch;
    }

    public CountDownLatch getRemoveLatch() {
        return removeLatch;
    }

    public CountDownLatch getChangeLatch() {
        return changeLatch;
    }
}
