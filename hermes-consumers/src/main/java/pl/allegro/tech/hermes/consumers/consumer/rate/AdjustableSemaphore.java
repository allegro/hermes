package pl.allegro.tech.hermes.consumers.consumer.rate;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.ThreadSafe;

/*
 *  CAUTION!
 *
 *  This is repackaged version of com.genius.concurrency.AdjustableSemaphore with few changes.
 *   http://blog.teamlazerbeez.com/2009/04/20/javas-semaphore-resizing/
 *
 *  Most notable changes:
 *  - set maxPermits in constructor
 *
 *  Implementation was deliberately not refactored for easier comparison with the original.
 *
 * */

/** A simple implementation of an adjustable semaphore. */
@ThreadSafe
public final class AdjustableSemaphore {

  /** Semaphore starts at 0 capacity; must be set by setMaxPermits before use. */
  private final ResizeableSemaphore semaphore;

  /**
   * How many permits are allowed as governed by this semaphore. Access must be synchronized on this
   * object.
   */
  private int maxPermits = 0;

  /** New instances should be configured with setMaxPermits(). */
  public AdjustableSemaphore(int maxPermits) {
    this.maxPermits = maxPermits;
    this.semaphore = new ResizeableSemaphore(maxPermits);
  }

  /*
   * Must be synchronized because the underlying int is not thread safe
   */

  /**
   * Set the max number of permits. Must be greater than zero.
   *
   * <p>Note that if there are more than the new max number of permits currently outstanding, any
   * currently blocking threads or any new threads that start to block after the call will wait
   * until enough permits have been released to have the number of outstanding permits fall below
   * the new maximum. In other words, it does what you probably think it should.
   */
  public synchronized void setMaxPermits(int newMax) {
    if (newMax < 1) {
      throw new IllegalArgumentException("Semaphore size must be at least 1," + " was " + newMax);
    }

    int delta = newMax - this.maxPermits;

    if (delta == 0) {
      return;
    } else if (delta > 0) {
      // new max is higher, so release that many permits
      this.semaphore.release(delta);
    } else {
      delta *= -1;
      // delta < 0.
      // reducePermits needs a positive #, though.
      this.semaphore.reducePermits(delta);
    }

    this.maxPermits = newMax;
  }

  /** Release a permit back to the semaphore. Make sure not to double-release. */
  public void release() {
    this.semaphore.release();
  }

  /**
   * Get a permit, blocking if necessary.
   *
   * @throws InterruptedException if interrupted while waiting for a permit
   */
  public void acquire() throws InterruptedException {
    this.semaphore.acquire();
  }

  public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
    return this.semaphore.tryAcquire(timeout, unit);
  }

  public int availablePermits() {
    return this.semaphore.availablePermits();
  }

  /**
   * A trivial subclass of <code>Semaphore</code> that exposes the reducePermits call to the parent
   * class. Doug Lea says it's ok...
   * http://osdir.com/ml/java.jsr.166-concurrency/2003-10/msg00042.html
   */
  private static final class ResizeableSemaphore extends Semaphore {
    private static final long serialVersionUID = 1L;

    /** Create a new semaphore with 0 permits. */
    ResizeableSemaphore(int maxPermits) {
      super(maxPermits);
    }

    @Override
    protected void reducePermits(int reduction) {
      super.reducePermits(reduction);
    }
  }
}
