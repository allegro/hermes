package pl.allegro.tech.hermes.tracker.consumers.deadletters;

public interface DeadRepository {
    void logDeadLetter(DeadMessage message, String reason);

    void close();
}
