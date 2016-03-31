package pl.allegro.tech.hermes.consumers.consumer.filtering;

import pl.allegro.tech.hermes.consumers.consumer.Message;

public class FilteredMessageHandler {
    public void handle(final FilterResult result, final Message message) {
        if (result.filtered) {
            // offsety
            // metryki
            // tracker
            // debug logging
        }

    }
}
