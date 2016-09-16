package pl.allegro.tech.hermes.management.utils

import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.read.ListAppender

class MockAppender extends ListAppender<LoggingEvent> {

    @Override
    void doAppend(LoggingEvent eventObject) {
        list.add(eventObject);
    }

    @Override
    String getName() {
        return "mock"
    }
}
