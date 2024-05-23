package pl.allegro.tech.hermes.consumers.supervisor.process;

public interface SentCallback {
    void onFinished(int partition, long offset);
}
