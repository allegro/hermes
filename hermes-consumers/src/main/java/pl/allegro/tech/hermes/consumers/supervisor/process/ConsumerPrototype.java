//package pl.allegro.tech.hermes.consumers.supervisor.process;
//
///*
//Założenia:
//1. Łatwe do zrozumienia ?
//    - informacja o ilości przetwarzanych wiadomości jest w wątku obsługującym subskrypcję
//    - jeszcze nie rozumiemy gdzie może się coś przepełnić/wybuchnąć
//
//2. Może wybiegać w przód z wysłanymi ale nie zacommitowanymi offsetami. Przesuwamy się tylko do określonego momentu,
//ustalonego magiczną liczbą w stylu 10K.
//    -
//3. Nie wpływa na inne subskrypcje:
//    - jedyny wspólny punkt to klient http ale go chronimy ratelimiterem ilości równoległych requestów
//
//4. Minimalizacja ilości informacji o przetworzonych ale nie zacommitowanych wiadomościach/offsetach
//(nigdy nie puchnie w nieskończoność).
//    - używamy tego samego algorytmu
//    - OffsetCommitter2 przechowuje ciągle `inflightOffsets` które zawiera offsety z poprzednich iteracji -> nie puchnie
//    - drainujemy kolejki w `offsetsSlots` za każdym przebiegiem i jednocześnie patrzymy na ich rozmiar przed pobraniem kolejnej wiadomości z kafki
//    - mamy kolejkę `offsetsSlots` która może puchnać jeżeli nie odpalamy committera, ale uwzględniamy ją przy wkładaniu nowych offsetów
//    - jak dobrać rozmiar kolejki `offsetsSlots`:
//        - 10K rps * 15s (zapas) = 150K
//5. Konfigurowalny delay pomiędzy commitami (nie każdy obrót pętli robi commit). -> Tak
// */
//
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.Semaphore;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.apache.kafka.clients.consumer.KafkaConsumer;
//import org.apache.kafka.clients.consumer.OffsetAndMetadata;
//import org.apache.kafka.common.TopicPartition;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//import org.jctools.queues.MpscArrayQueue;
//import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetCommitter2;
//import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartition;
//import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
//import pl.allegro.tech.hermes.consumers.queue.FullDrainMpscQueue;
//import pl.allegro.tech.hermes.consumers.queue.MonitoredMpscQueue;
//import pl.allegro.tech.hermes.consumers.queue.MpscQueue;
//
//public class ConsumerPrototype {
//    private final NewMessageSender client = new NewMessageSender();
//    private final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of());
//    private final OffsetsSlots offsetsSlots = new OffsetsSlots(); // should be data structure
//    private final int largerThreshold = 20_000;
//    private final KafkaMessageReceiver messageReceiver = new KafkaMessageReceiver();
//    private final OffsetCommitter2 offsetCommitter = new OffsetCommitter2();
//
//    public void run() throws InterruptedException {
//        while (true) {
//            do {
//                if (isReadyToCommit()) { // check if last commit was earlier than X, then commit
//                    consumer.commitSync(calculateOffsetsToBeCommitted());
//                }
//                // should be atomic
//                // throttling semaphore
//            } while (!offsetsSlots.hasSpace() && offsetsSlots.hasFreeSlots()); // TODO
//
//            Optional<ConsumerRecord<String, String>> record = messageReceiver.next();
//            record.ifPresent(
//                    r -> {
//                        offsetsSlots.addSlot(r.partition(), r.offset())
//                        client.send(new SentCallback() {
//                            @Override
//                            public void onFinished(int partition, long offset) {
//                                // can't be blocking
//                                offsetsSlots.markAsSent(partition, offset); // TODO
//                            }
//                        }, r); // non blocking
//                    }
//            );
//        }
//    }
//
//    private Map<TopicPartition, OffsetAndMetadata> calculateOffsetsToBeCommitted() {
//        Map<TopicPartition, OffsetAndMetadata> result = new HashMap<>();
//        MpscQueue<SubscriptionPartitionOffset> drainedSent = offsetsSlots.drainSent();
//        offsetCommitter.calculateOffsetsToBeCommitted(offsetsSlots.getInflight(), drainedSent);
//
//        return result;
//    }
//
//    private boolean isReadyToCommit() {
//        return true;
//    }
//
//    enum STATE {
//        INFLIGHT,
//        DELIVERED
//    }
//
//    static class Slot {
//        private STATE state;
//
//        Slot(STATE state) {
//            this.state = state;
//        }
//
//        STATE getState() {
//            return state;
//        }
//    }
//
//    static class OffsetsSlots {
//
//        private final ConcurrentHashMap<SubscriptionPartitionOffset, Slot> slots = new ConcurrentHashMap<>();
//        private final Semaphore inflightSemaphore = new Semaphore(60);
//        private final Semaphore totalOffsetsCountSemaphore = new Semaphore(20_000);
//
//
//        // used by sender thread
//        void markAsSent(int partition, long offset) throws InterruptedException {
//            inflightSemaphore.release();
//            SubscriptionPartitionOffset key = new SubscriptionPartitionOffset(
//                    null, offset
//            );
//            slots.get(key).state = STATE.DELIVERED;
//        }
//
//        boolean hasSpace() throws InterruptedException {
//            int permits = inflightSemaphore.availablePermits();
//            if (permits == 0) {
//                Thread.sleep(100);
//                return false;
//            } else {
//                return true;
//            }
//        }
//
//        // used by consumer thread
//        boolean hasFreeSlots() throws InterruptedException {
//            int permits = totalOffsetsCountSemaphore.availablePermits();
//            if (permits == 0) {
//                Thread.sleep(100);
//                return false;
//            } else {
//                return true;
//            }
//        }
////            inflightSemaphore.acquire();
////            deliveredSemaphore.acquire();
//
//        // used by consumer thread
//        void addSlot(int partition, long offset) throws InterruptedException {
//            totalOffsetsCountSemaphore.acquire();
//            inflightSemaphore.acquire();
//            slots.put(new SubscriptionPartitionOffset(
//                    new SubscriptionPartition(null, null, partition, 0),
//                    offset
//            ), new Slot(STATE.INFLIGHT));
//        }
//
//
//        // used by consumer thread
//        // lists have to be sorted in ascending order
//        public MpscQueue<SubscriptionPartitionOffset> drainSent() {
//            int permitsReleased = 0;
//            MpscQueue<SubscriptionPartitionOffset> drained = new FullDrainMpscQueue<>(100000);
//            for (Map.Entry<SubscriptionPartitionOffset, Slot> entry : slots.entrySet()) {
//                if (entry.getValue().getState() == STATE.DELIVERED) {
//                    slots.remove(entry.getKey());
//                    drained.offer(entry.getKey());
//                    permitsReleased++;
//                }
//            }
//            totalOffsetsCountSemaphore.release(permitsReleased);
//            return drained;
//        }
//
//        MpscQueue<SubscriptionPartitionOffset> getInflight() {
//            MpscQueue<SubscriptionPartitionOffset> inflight = new FullDrainMpscQueue<>(60);
//            for (Map.Entry<SubscriptionPartitionOffset, Slot> entry : slots.entrySet()) {
//                if (entry.getValue().getState() == STATE.DELIVERED) {
//                    slots.remove(entry.getKey());
//                    drained.offer(entry.getKey());
//                    permitsReleased++;
//                }
//            }
//            return null;
//        }
//    }
//}
//
//
//// inflight = [1, 2, 3, 4, 5] deliveredQueue = [........................................]
//// inflight = [1, 2, 4] deliveredQueue = [3, 5] -> [], maxDelivered = 5
//// inflight = [1, 2] deliveredQueue = [4] -> [], maxDelivered = 5
//// inflight = [2] deliveredQueue = [1] -> [], maxDelivered = 5, min(2, maxDelivered) = 2
//
//// jest kompresja deliveredOffsets, ale nie ma kompresji dla inflights
