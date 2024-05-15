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
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public class ConsumerPrototype {
    private final NewMessageSender client;
    private final KafkaConsumer<String, String> consumer;
    private final OffsetsSlots offsetsSlots = new OffsetsSlots(); // should be data structure
    private int largerThreshold = 20_000;
    private int concurrentRequests = 0;
    private KafkaMessageReceiver messageReceiver = new KafkaMessageReceiver();

    public void run() {
        while (true) {
            // throttling from layers above
            do {
                // should sleep to not exhaust CPU
                if (isReadyToCommit()) {
                    consumer.commitSync(offsetsToBeCommitted);
                }
                // should be atomic, maybe just check if hasSpace and call offsetsSlots.add(record.offset()
            } while (!hasSpace() || !offsetsSlots.tryAddSlot(record.offset()));

            Optional<ConsumerRecord<String, String>> record = messageReceiver.next();
            record.ifPresent(r -> offsetsSlots.add(r.offset()));
            client.send(new SentCallback() {
                @Override
                public void onFinished(int offset) {
                    // can't be blocking
                    offsetsSlots.markAsSent(offset);
                }
            }, record); // non blocking
            break;
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

        void add(int offset) {

        }

        boolean hasFreeSlots() {
        }
    }

}
        }
