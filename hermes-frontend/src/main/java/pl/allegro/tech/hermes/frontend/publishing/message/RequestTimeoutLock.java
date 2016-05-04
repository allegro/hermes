package pl.allegro.tech.hermes.frontend.publishing.message;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RequestTimeoutLock {

    private Lock lock = new ReentrantLock();

    public boolean tryLock() {
        return lock.tryLock();
    }

    public void unlock() {
        lock.unlock();
    }

    public void lock() {
        lock.lock();
    }
}
