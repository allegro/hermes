package pl.allegro.tech.hermes.common.cache.queue;

import java.util.Arrays;

public class QueueTask implements Runnable {
    private final Runnable operation;
    private final Object[] payload;

    public QueueTask(Runnable operation, Object... payload) {
        this.operation = operation;
        this.payload = payload;
    }

    @Override
    public void run() {
        operation.run();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueueTask that = (QueueTask) o;
        return Arrays.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(payload);
    }
}
