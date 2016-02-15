package pl.allegro.tech.hermes.consumers.consumer.batch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class DirectBufferPoolTest {
    long totalMemory = 10*1024;
    int poolableSize = 1024;

    DirectBufferPool pool;
    ByteBuffer buffer;

    @Before
    public void setup() {
        this.pool = new DirectBufferPool(totalMemory, poolableSize, false);
    }


    @After
    public void cleanup() {
        if (buffer != null) pool.deallocate(buffer);
    }

    @Test
    public void shouldAllocateGivenAmountOfBytes() throws InterruptedException {
        // given
        int size = 512;

        // when
        buffer = pool.allocate(size);

        // then
        assertEquals("Buffer size should equal requested size.", size, buffer.limit());
        assertEquals("Unallocated memory should have shrunk", totalMemory - size, pool.unallocatedMemory());
        assertEquals("Available memory should have shrunk", totalMemory - size, pool.availableMemory());
    }

    @Test
    public void shouldDeallocateAllMemoryFromBuffer() throws InterruptedException {
        // given
        buffer = pool.allocate(512).putInt(1);
        buffer.flip();

        // when
        pool.deallocate(buffer);

        // then
        assertEquals("All memory should be available", totalMemory, pool.availableMemory());
        assertEquals("Nothing is on a free list", totalMemory, pool.unallocatedMemory());
    }

    @Test
    public void shouldRecycleBuffersOfPoolableSize() throws InterruptedException {
        // given
        pool.deallocate(pool.allocate(poolableSize));

        // when
        buffer = pool.allocate(poolableSize);

        // then
        assertEquals("Recycled buffer should be cleared.", 0, buffer.position());
        assertEquals("Recycled buffer should be cleared.", buffer.capacity(), buffer.limit());
        assertEquals("Still a single buffer on the free list", totalMemory - poolableSize, pool.unallocatedMemory());
    }

    @Test
    public void shouldDeallocateNonStandardBufferSize() throws InterruptedException {
        // given
        buffer = pool.allocate(2 * poolableSize);

        // when
        pool.deallocate(buffer);

        // then
        assertEquals("All memory should be available", totalMemory, pool.availableMemory());
        assertEquals("Non-standard size didn't go to the free list.", totalMemory, pool.unallocatedMemory());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllocateMoreMemoryThanTotalMemoryWeHave() throws InterruptedException {
        // given
        DirectBufferPool pool = new DirectBufferPool(1024, 512, true);

        // when
        pool.allocate(1025);
    }

    @Test(expected = BufferOverflowException.class)
    public void shouldThrowExceptionOnBufferExhaustion() throws InterruptedException {
        // given
        DirectBufferPool pool = new DirectBufferPool(1024, 512, false);

        // when
        pool.allocate(512);
        pool.allocate(768);
    }

    @Test
    public void shouldBlockOnAllocationUntilMemoryIsAvailable() throws Exception {
        // given
        int total = 5 * 1024;
        DirectBufferPool pool = new DirectBufferPool(total, 1024, true);
        ByteBuffer buffer = pool.allocate(1024);

        // when
        CountDownLatch doDeallocation = asyncDeallocate(pool, buffer);
        CountDownLatch allocation = asyncAllocate(pool, total);

        // then
        assertEquals("Allocation shouldn't have happened yet, waiting on memory.", 1L, allocation.getCount());
        doDeallocation.countDown();
        allocation.await();
    }

    private CountDownLatch asyncDeallocate(final DirectBufferPool pool, final ByteBuffer buffer) {
        final CountDownLatch latch = new CountDownLatch(1);
        Thread thread = new Thread() {
            public void run() {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                pool.deallocate(buffer);
            }
        };
        thread.start();
        return latch;
    }

    private CountDownLatch asyncAllocate(final DirectBufferPool pool, final int size) {
        final CountDownLatch completed = new CountDownLatch(1);
        Thread thread = new Thread() {
            public void run() {
                try {
                    pool.allocate(size);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    completed.countDown();
                }
            }
        };
        thread.start();
        return completed;
    }

    @Test
    public void shouldSurviveHammeringOfLotsOfThreadsOnBufferPool() throws Exception {
        // given
        int numThreads = 10;
        final int iterations = 50000;
        final int poolableSize = 1024;
        final int totalMemory = numThreads / 2 * poolableSize;
        final DirectBufferPool pool = new DirectBufferPool(totalMemory, poolableSize, true);

        List<StressTestThread> threads = IntStream.range(0, numThreads)
                .mapToObj(i -> new StressTestThread(pool, iterations))
                .collect(Collectors.toList());

        // when
        threads.forEach(StressTestThread::start);
        threads.forEach(StressTestThread::joinQuietly);

        // then
        threads.forEach(thread ->
                assertTrue("Thread should have completed all iterations successfully.", thread.success.get()));
        assertEquals(totalMemory, pool.availableMemory());
    }

    public static class StressTestThread extends Thread {
        private final int iterations;
        private final DirectBufferPool pool;
        public final AtomicBoolean success = new AtomicBoolean(false);
        Random random = new Random();

        public StressTestThread(DirectBufferPool pool, int iterations) {
            this.iterations = iterations;
            this.pool = pool;
        }

        public void run() {
            try {
                for (int i = 0; i < iterations; i++) {
                    int size = random.nextBoolean()? pool.poolableSize() : random.nextInt((int) pool.totalMemory());
                    ByteBuffer buffer = pool.allocate(size);
                    pool.deallocate(buffer);
                }
                success.set(true);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void joinQuietly() {
            try {
                join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
