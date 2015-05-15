package pl.allegro.tech.hermes.consumers.test;

import pl.allegro.tech.hermes.consumers.message.tracker.SendingMessageTracker;
import pl.allegro.tech.hermes.consumers.message.tracker.NoOperationSendingTracker;
import pl.allegro.tech.hermes.consumers.message.tracker.Trackers;

import static org.mockito.Mockito.mock;

public class TestTrackers extends Trackers {

    public TestTrackers() {
        super(mock(SendingMessageTracker.class), mock(NoOperationSendingTracker.class));
    }
}
