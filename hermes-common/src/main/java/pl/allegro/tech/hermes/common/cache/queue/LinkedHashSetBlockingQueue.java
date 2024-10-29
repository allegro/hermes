package pl.allegro.tech.hermes.common.cache.queue;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A blocking queue implementation backed by a linked hash set for predictable iteration order and
 * constant time addition, removal and contains operations.
 *
 * <p>Author: Sebastian Schaffert Project: apache.marmotta
 */
public class LinkedHashSetBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

  private int capacity = Integer.MAX_VALUE;

  /** Current number of elements */
  private final AtomicInteger count = new AtomicInteger(0);

  /** Lock held by take, poll, etc */
  private final ReentrantLock takeLock = new ReentrantLock();

  /** Wait queue for waiting takes */
  private final Condition notEmpty = takeLock.newCondition();

  /** Lock held by put, offer, etc */
  private final ReentrantLock putLock = new ReentrantLock();

  /** Wait queue for waiting puts */
  private final Condition notFull = putLock.newCondition();

  private final LinkedHashSet<E> delegate;

  public LinkedHashSetBlockingQueue() {
    delegate = new LinkedHashSet<E>();
  }

  public LinkedHashSetBlockingQueue(int capacity) {
    this.delegate = new LinkedHashSet<E>(capacity);
    this.capacity = capacity;
  }

  @Override
  public boolean offer(E e) {
    if (e == null) throw new NullPointerException();
    final AtomicInteger count = this.count;
    if (count.get() == capacity) return false;
    int c = -1;
    final ReentrantLock putLock = this.putLock;
    putLock.lock();
    try {
      if (count.get() < capacity) {
        final boolean wasAdded = enqueue(e);
        c = wasAdded ? count.getAndIncrement() : count.get();
        if (c + 1 < capacity) notFull.signal();
      }
    } finally {
      putLock.unlock();
    }
    if (c == 0) signalNotEmpty();
    return c >= 0;
  }

  @Override
  public void put(E e) throws InterruptedException {
    if (e == null) throw new NullPointerException();

    int c = -1;
    final ReentrantLock putLock = this.putLock;
    final AtomicInteger count = this.count;
    putLock.lockInterruptibly();
    try {
      while (count.get() == capacity) {
        notFull.await();
      }
      final boolean wasAdded = enqueue(e);
      c = wasAdded ? count.getAndIncrement() : count.get();
      if (c + 1 < capacity) notFull.signal();
    } finally {
      putLock.unlock();
    }
    if (c == 0) signalNotEmpty();
  }

  @Override
  public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
    if (e == null) throw new NullPointerException();
    long nanos = unit.toNanos(timeout);
    int c = -1;
    final ReentrantLock putLock = this.putLock;
    final AtomicInteger count = this.count;
    putLock.lockInterruptibly();
    try {
      while (count.get() == capacity) {

        if (nanos <= 0) return false;
        nanos = notFull.awaitNanos(nanos);
      }
      final boolean wasAdded = enqueue(e);
      c = wasAdded ? count.getAndIncrement() : count.get();
      if (c + 1 < capacity) notFull.signal();
    } finally {
      putLock.unlock();
    }
    if (c == 0) signalNotEmpty();
    return true;
  }

  @Override
  public E take() throws InterruptedException {
    E x;
    int c = -1;
    final AtomicInteger count = this.count;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lockInterruptibly();
    try {
      while (count.get() == 0) {
        notEmpty.await();
      }
      x = dequeue();
      c = count.getAndDecrement();
      if (c > 1) notEmpty.signal();
    } finally {
      takeLock.unlock();
    }
    if (c == capacity) signalNotFull();
    return x;
  }

  @Override
  public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    E x = null;
    int c = -1;
    long nanos = unit.toNanos(timeout);
    final AtomicInteger count = this.count;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lockInterruptibly();
    try {
      while (count.get() == 0) {
        if (nanos <= 0) return null;
        nanos = notEmpty.awaitNanos(nanos);
      }
      x = dequeue();
      c = count.getAndDecrement();
      if (c > 1) notEmpty.signal();
    } finally {
      takeLock.unlock();
    }
    if (c == capacity) signalNotFull();
    return x;
  }

  @Override
  public int remainingCapacity() {
    return Integer.MAX_VALUE - size();
  }

  @Override
  public int drainTo(Collection<? super E> c) {
    return drainTo(c, Integer.MAX_VALUE);
  }

  @Override
  public int drainTo(Collection<? super E> c, int maxElements) {
    if (c == null) throw new NullPointerException();
    if (c == this) throw new IllegalArgumentException();
    boolean signalNotFull = false;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
      int n = Math.min(maxElements, count.get());
      Iterator<E> it = delegate.iterator();
      for (int i = 0; i < n && it.hasNext(); i++) {
        E x = it.next();
        c.add(x);
      }
      count.getAndAdd(-n);
      return n;
    } finally {
      takeLock.unlock();
      if (signalNotFull) signalNotFull();
    }
  }

  @Override
  public E poll() {
    final AtomicInteger count = this.count;
    if (count.get() == 0) return null;
    E x = null;
    int c = -1;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
      if (count.get() > 0) {
        x = dequeue();
        c = count.getAndDecrement();
        if (c > 1) notEmpty.signal();
      }
    } finally {
      takeLock.unlock();
    }
    if (c == capacity) signalNotFull();
    return x;
  }

  @Override
  public E peek() {
    if (count.get() == 0) return null;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
      Iterator<E> it = delegate.iterator();
      if (it.hasNext()) {
        return it.next();
      } else {
        return null;
      }
    } finally {
      takeLock.unlock();
    }
  }

  /**
   * Creates a node and links it at end of queue.
   *
   * @param x the item
   * @return <code>true</code> if this set did not already contain <code>x</code>
   */
  private boolean enqueue(E x) {
    synchronized (delegate) {
      return delegate.add(x);
    }
  }

  /**
   * Removes a node from head of queue.
   *
   * @return the node
   */
  private E dequeue() {
    synchronized (delegate) {
      Iterator<E> it = delegate.iterator();
      E x = it.next();
      it.remove();
      return x;
    }
  }

  /** Lock to prevent both puts and takes. */
  void fullyLock() {
    putLock.lock();
    takeLock.lock();
  }

  /** Unlock to allow both puts and takes. */
  void fullyUnlock() {
    takeLock.unlock();
    putLock.unlock();
  }

  /**
   * Signals a waiting take. Called only from put/offer (which do not otherwise ordinarily lock
   * takeLock.)
   */
  private void signalNotEmpty() {
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
      notEmpty.signal();
    } finally {
      takeLock.unlock();
    }
  }

  /** Signals a waiting put. Called only from take/poll. */
  private void signalNotFull() {
    final ReentrantLock putLock = this.putLock;
    putLock.lock();
    try {
      notFull.signal();
    } finally {
      putLock.unlock();
    }
  }

  /** Tells whether both locks are held by current thread. */
  boolean isFullyLocked() {
    return (putLock.isHeldByCurrentThread() && takeLock.isHeldByCurrentThread());
  }

  @Override
  public Iterator<E> iterator() {
    final Iterator<E> it = delegate.iterator();
    return new Iterator<E>() {
      @Override
      public boolean hasNext() {
        fullyLock();
        try {
          return it.hasNext();
        } finally {
          fullyUnlock();
        }
      }

      @Override
      public E next() {
        fullyLock();
        try {
          return it.next();
        } finally {
          fullyUnlock();
        }
      }

      @Override
      public void remove() {
        fullyLock();
        try {
          it.remove();

          // remove counter
          count.getAndDecrement();
        } finally {
          fullyUnlock();
        }
      }
    };
  }

  @Override
  public int size() {
    return count.get();
  }

  @Override
  public boolean remove(Object o) {
    if (o == null) return false;

    fullyLock();
    try {
      if (delegate.remove(o)) {
        if (count.getAndDecrement() == capacity) {
          notFull.signal();
        }
        return true;
      }
    } finally {
      fullyUnlock();
    }

    return false;
  }

  @Override
  public void clear() {
    fullyLock();
    try {
      delegate.clear();
      count.set(0);
    } finally {
      fullyUnlock();
    }
  }
}
