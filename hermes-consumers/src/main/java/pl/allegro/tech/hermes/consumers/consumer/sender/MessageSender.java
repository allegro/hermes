package pl.allegro.tech.hermes.consumers.consumer.sender;

import com.google.common.util.concurrent.ListenableFuture;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;

public interface MessageSender {

    ListenableFuture<MessageSendingResult> send(Message message);

    void stop();
}
