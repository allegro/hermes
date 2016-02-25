package pl.allegro.tech.hermes.consumers.consumer.result.undelivered;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class UndeliveredMessageHandlers {

    private final List<UndeliveredMessageHandler> undeliveredMessageHandlerList  = new ArrayList<>();

    public void addHandler(LogUndeliveredMessageHandler logUndeliveredMessageHandler) {
        undeliveredMessageHandlerList.add(logUndeliveredMessageHandler);
    }

    public void addHandlers(List<UndeliveredMessageHandler> undeliveredMessageHandlers) {
        undeliveredMessageHandlerList.addAll(undeliveredMessageHandlers);
    }

    public void handleDiscarded(Message message, Subscription subscription, MessageSendingResult result) {
        undeliveredMessageHandlerList.forEach(handler -> handler.handleDiscarded(message, subscription, result));
    }

}
