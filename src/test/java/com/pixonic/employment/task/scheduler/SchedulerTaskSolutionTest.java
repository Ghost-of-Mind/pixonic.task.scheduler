package com.pixonic.employment.task.scheduler;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.synchronizedList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public abstract class SchedulerTaskSolutionTest {

    public static final long ACCEPTABLE_DELTA_MILLIS = 100L;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected abstract SchedulerTaskSolution getSolution();


    @Test
    public void testInvokeCallableAtSpecifiedTime() throws InterruptedException {
        final SchedulerTaskSolution solution = getSolution();
        final AtomicReference<String> taskFailResultMessage = new AtomicReference<>(null);
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final long delayMillis = ACCEPTABLE_DELTA_MILLIS * 2;
        final LocalDateTime requestedTime = now().plus(delayMillis, MILLIS);
        solution.schedule(
            () -> {
                final long diff = MILLIS.between(requestedTime, now());
                if (diff < 0) {
                    taskFailResultMessage.set("Task has been invoked ahead of time: diff millis: " + diff);
                } else if (ACCEPTABLE_DELTA_MILLIS < diff) {
                    taskFailResultMessage.set("Task has been invoked too late: diff millis: " + diff);
                }
                finishLatch.countDown();
                return null;
            },
            requestedTime
        );
        assertTrue("Timeout occurred while task completion awaiting", finishLatch.await(delayMillis + 2 * ACCEPTABLE_DELTA_MILLIS, MILLISECONDS));
        final String failMessageOrNull = taskFailResultMessage.get();
        assertNull(failMessageOrNull, failMessageOrNull);
    }


    @Test
    public void testInvocationOrder() throws InterruptedException {
        final SchedulerTaskSolution solution = getSolution();
        final List<Integer> order = synchronizedList(new ArrayList<>());
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(3);
        final LocalDateTime now = now();
        solution.schedule(() -> {
                startLatch.await(); // simulate overloading
                return null;
            },
            now
        );
        solution.schedule(() -> { order.add(1); finishLatch.countDown(); return null; }, now.plusNanos(200L));
        solution.schedule(() -> { order.add(2); finishLatch.countDown(); return null; }, now.plusNanos(100L));
        solution.schedule(() -> { order.add(3); finishLatch.countDown(); return null; }, now.plusNanos(100L));
        Thread.sleep(0, 300);
        startLatch.countDown();
        assertTrue("Timeout occurred while tasks completion awaiting", finishLatch.await(2 * ACCEPTABLE_DELTA_MILLIS, MILLISECONDS));
        assertEquals(Arrays.asList(2, 3, 1), order);
    }


    @Test
    public void testSchedulingFromDifferentThreads() throws InterruptedException, ExecutionException, TimeoutException {
        final SchedulerTaskSolution solution = getSolution();
        final int threadsCount = Runtime.getRuntime().availableProcessors();
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(threadsCount);
        final ExecutorService differentThreadsCaller = newFixedThreadPool(threadsCount);
        final Collection<Future> futures = new ArrayList<>(threadsCount);
        final LocalDateTime now = now();
        for (int i = 0; i < threadsCount; i++) {
            futures.add(differentThreadsCaller.submit(() -> {
                startLatch.await();
                solution.schedule(() -> { finishLatch.countDown(); return null; }, now);
                return null;
            }));
        }
        startLatch.countDown();
        assertTrue("Timeout occurred while tasks completion awaiting", finishLatch.await(2 * ACCEPTABLE_DELTA_MILLIS, MILLISECONDS));
        for (Future future : futures) {
            future.get(0L /* must already finish */, MILLISECONDS); // check no exceptions have occurred while scheduling
        }
        differentThreadsCaller.shutdownNow();
        if (!differentThreadsCaller.awaitTermination(1L, SECONDS)) {
            logger.warn("{} shutdowns too long", differentThreadsCaller);
        }
    }


    @Test(expected = NullPointerException.class)
    public void testNullTask() {
        final SchedulerTaskSolution solution = getSolution();
        solution.schedule(null, now());
    }

    @Test(expected = NullPointerException.class)
    public void testNullTime() {
        final SchedulerTaskSolution solution = getSolution();
        solution.schedule(() -> null, null);
    }

}
