package com.github.ghost_of_mind.pixonic.task.scheduler;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.pixonic.employment.task.scheduler.SchedulerTaskSolutionTest.ACCEPTABLE_DELTA_MILLIS;
import static java.time.LocalDateTime.now;
import static java.util.concurrent.Executors.defaultThreadFactory;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;


public class SchedulerServiceImplTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private @Nullable SchedulerService scheduler;


    @Test
    public void testThreadFactoryUsage() throws InterruptedException {
        final AtomicReference<Thread> createdThread = new AtomicReference<>(null);
        final AtomicReference<Thread> usedThread = new AtomicReference<>(null);
        final CountDownLatch finishLatch = new CountDownLatch(2);
        scheduler = new SchedulerService(runnable -> {
            final Thread thread = defaultThreadFactory().newThread(runnable);
            Assert.assertTrue("Second thread was requested from thread factory", createdThread.compareAndSet(null, thread));
            return thread;
        });
        assertNull("Thread was requested from thread factory before first task has been scheduled", createdThread.get());
        scheduler.schedule(() -> {
                usedThread.set(Thread.currentThread());
                finishLatch.countDown();
                return null;
            },
            now()
        );
        // check if thread factory has been called only once
        scheduler.schedule(() -> { finishLatch.countDown(); return null; }, now());
        assertTrue("Timeout occurred while task completion awaiting", finishLatch.await(2 * ACCEPTABLE_DELTA_MILLIS, MILLISECONDS));
        assertSame("Scheduler uses not same thread that thread factory created", usedThread.get(), createdThread.get());
    }


    @Test(expected = NullPointerException.class)
    public void testNullThreadFactory() {
        scheduler = new SchedulerService(null);
    }


    @After
    public void afterTest() throws InterruptedException {
        if (scheduler != null) {
            scheduler.shutdownNow();
            if (!scheduler.awaitTermination(1L, TimeUnit.SECONDS)) {
                logger.warn("{} shutdowns too long", scheduler);
            }
            scheduler = null;
        }
    }

}
