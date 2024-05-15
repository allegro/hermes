package pl.allegro.tech.hermes.consumers.supervisor.process;

/*
Założenia:
1. Łatwe do zrozumienia
2. Może wybiegać w przód z wysłanymi ale nie zacommitowanymi offsetami. Przesuwamy się tylko do określonego momentu,
ustalonego magiczną liczbą w stylu 10K.
3. Nie wpływa na inne subskrypcje
4. Minimalizacja ilości informacji o przetworzonych ale nie zacommitowanych wiadomościach/offsetach
(nigdy nie puchnie w nieskończoność).
5. Konfigurowalny delay pomiędzy commitami (nie każdy obrót pętli robi commit).
 */

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public class ConsumerPrototype {
    private final NewMessageSender client = new NewMessageSender();
    private final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of());
    private final OffsetsSlots offsetsSlots = new OffsetsSlots(); // should be data structure
    private final int largerThreshold = 20_000;
    private final KafkaMessageReceiver messageReceiver = new KafkaMessageReceiver();

    public void run() {
        while (true) {
            do {
                // sleep to not exhaust CPU
                if (isReadyToCommit()) {
                    consumer.commitSync(offsetsToBeCommitted);
                }
                // should be atomic
                // throttling semaphore
            } while (!hasSpace() && offsetsSlots.hasFreeSlots());

            Optional<ConsumerRecord<String, String>> record = messageReceiver.next();
            record.ifPresentOrElse(
                    r -> {
                        if (!offsetsSlots.addSlot(r.offset())) {
                            client.send(new SentCallback() {
                                @Override
                                public void onFinished(int offset) {
                                    // can't be blocking
                                    offsetsSlots.markAsSent(offset);
                                }
                            }, r); // non blocking
                        }
                    }, () -> {
                        // release semaphore
                    }
            );
        }
    }


    private boolean weAreTooFarInFutureOffsets() {
        // if the collection is concurrent be careful about size() method as it is approximate value
        return (offsetsToBeCommitted.size()) < largerThreshold;
    }

    private boolean hasSpace() {
        return false;
    }

    private boolean isReadyToCommit() {
        return Instant.now() - lastCommitTime >= delta; // check if enough time passed
    }

    static class OffsetsSlots {
        void markAsSent(int offset) {

        }

        boolean tryAddSlot(long offset) {
            return true;
        }

        boolean hasFreeSlots() {
            return true;
        }

        boolean addSlot(long offset) {
            return true;
        }
    }

}
