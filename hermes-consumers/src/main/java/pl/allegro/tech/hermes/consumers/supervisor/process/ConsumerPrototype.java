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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ConsumerPrototype {
    private final NewMessageSender client = new NewMessageSender();
    private final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of());
    private final OffsetsSlots offsetsSlots = new OffsetsSlots(); // should be data structure
    private final int largerThreshold = 20_000;
    private final KafkaMessageReceiver messageReceiver = new KafkaMessageReceiver();

    // todo: fill this map on start
    private final Map<TopicPartition, Long> lastCommitted = new HashMap<>();

    public void run() {
        while (true) {
            do {
                // sleep to not exhaust CPU
                if (isReadyToCommit()) {
                    consumer.commitSync(calculateOffsetsToBeCommitted());
                }
                // should be atomic
                // throttling semaphore
            } while (!hasSpace() && offsetsSlots.hasFreeSlots());

            Optional<ConsumerRecord<String, String>> record = messageReceiver.next();
            record.ifPresentOrElse(
                    r -> {
                        if (!offsetsSlots.addSlot(r.partition(), r.offset())) {
                            client.send(new SentCallback() {
                                @Override
                                public void onFinished(int partition, long offset) {
                                    // can't be blocking
                                    offsetsSlots.markAsSent(partition, offset);
                                }
                            }, r); // non blocking
                        }
                    }, () -> {
                        // release semaphore
                    }
            );
        }
    }

    private Map<TopicPartition, OffsetAndMetadata> calculateOffsetsToBeCommitted() {
        Map<TopicPartition, OffsetAndMetadata> result = new HashMap<>();

        Map<Integer, List<Long>> sentOffsets = offsetsSlots.getSent();

        for (Map.Entry<Integer, List<Long>> entry : sentOffsets.entrySet()) {
            // lastCommitted = 0
            // [ 1 | 2 | 3 | 4 | 5 ]

            // lastCommitted = 5
            // []

            // todo: is this case possible? perhaps ConsumerPartitionAssignmentState is important here
            // lastCommitted = 5
            // [ 1 | 2 | 3 | ... ]

            // lastCommitted = 5
            // [ 7 | 8 | 9 ]

            // lastCommitted = 5
            // [ 6 | 7 | 10 ]
            TopicPartition partition = new TopicPartition("<topic>", entry.getKey());
            // 1 | 2 | 3 | 4 | 5
            // []
            // [ 7 | 8 | 9 ]
            // [ 6 | 7 | 10 ]
            List<Long> offsets = entry.getValue();

            // 0
            // prev = 5
            // prev = 5
            // prev = 5
            long prev = lastCommitted.get(partition);
            for (long current : offsets) {
                if (current > prev + 1) {
                    break;
                } else {
                    prev = current;
                }
            }

            // prev = 5
            // prev = 7
            if (prev != lastCommitted.get(partition)) {
                result.put(partition, new OffsetAndMetadata(prev));
                offsetsSlots.free(partition, prev);
                lastCommitted.put(partition, prev);
            }
        }

        return result;
    }

    private boolean hasSpace() {
        return false;
    }

    private boolean isReadyToCommit() {
        return Instant.now() - lastCommitTime >= delta; // check if enough time passed
    }

    static class OffsetsSlots {

        // used by sender thread
        void markAsSent(int partition, long offset) {

        }

        // used by consumer thread
        boolean hasFreeSlots() {
            return true;
        }

        // used by consumer thread
        boolean addSlot(int partition, long offset) {
            return true;
        }

        // used by consumer thread
        public void free(TopicPartition partition, long prev) {

        }

        // used by consumer thread
        // lists have to be sorted in ascending order
        public Map<Integer, List<Long>> getSent() {
            return null;
        }
    }
}
