package com.github.ghost_of_mind.pixonic.task.scheduler;

import com.pixonic.employment.task.scheduler.SchedulerTaskSolution;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static java.time.temporal.ChronoUnit.NANOS;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;


/**
 * @apiNote Must be shutdown when not needed anymore. This aspect is implemented same to {@link ExecutorService}.
 */
public class SchedulerService implements SchedulerTaskSolution {

    private final ThreadPoolExecutor scheduler;
    private final AtomicLong sequence = new AtomicLong(Long.MIN_VALUE);
    private final Executor worker;


    public SchedulerService(final ThreadFactory threadFactory) {
        this(threadFactory, CurrentThreadExecutor.instance());
    }

    /**
     * @implNote when external worker is used there are no guaranties in task execution order,
     * only task passing to the {@code worker} will be ordered properly.
     */
    protected SchedulerService(final ThreadFactory threadFactory, final Executor worker) {
        this.worker = requireNonNull(worker, "worker");
        requireNonNull(threadFactory, "threadFactory");
        this.scheduler = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            // we can convert DelayQueue<ScheduledTask> to BlockingQueue<Runnable> with raw-types
            // but it may cause some implicit problems on future Java platform changes
            // so until we have no performance issues here we should use explicit adapter:
            new BlockingQueueOfRunnableAdapter<>(new DelayQueue<>(), this::wrapRunnable),
            threadFactory
        );
    }


    @Override
    public void schedule(final Callable<?> task, final LocalDateTime at) {
        requireNonNull(task, "task");
        requireNonNull(at, "at");
        scheduler.prestartAllCoreThreads(); // ThreadPoolExecutor will be enqueue tasks only if all core threads exist
        scheduler.execute(new ScheduledTask(new FutureTask<>(task), at.atZone(ZoneId.systemDefault()).toInstant()));
    }


    private ScheduledTask wrapRunnable(final Runnable runnable) {
        if (runnable instanceof ScheduledTask) {
            return (ScheduledTask) runnable;
        }
        return new ScheduledTask(runnable, Instant.now());
    }



    private class ScheduledTask implements Delayed, Runnable {

        private Runnable task;
        private Instant startInstant;
        private long sequenceNumber;

        public ScheduledTask(final Runnable task, final Instant startInstant) {
            this.task = task;
            this.startInstant = startInstant;
            this.sequenceNumber = sequence.getAndIncrement();
        }

        @Override
        public long getDelay(final TimeUnit unit) {
            final Instant now = Instant.now();
            long durationNanos;
            try {
                durationNanos = NANOS.between(now, startInstant);
            } catch (final ArithmeticException e) { // long overflowed
                durationNanos = now.isBefore(startInstant) ? Long.MAX_VALUE : Long.MIN_VALUE;
            }
            return unit.convert(durationNanos, NANOSECONDS);
        }

        @Override
        public int compareTo(final Delayed another) {
            if (another == this) {
                return 0;
            }
            if (another instanceof ScheduledTask) {
                final ScheduledTask anotherTask = (ScheduledTask) another;
                final int result = startInstant.compareTo(anotherTask.startInstant);
                return result != 0 ? result : Long.compare(sequenceNumber, anotherTask.sequenceNumber);
            }
            return Long.compare(getDelay(NANOSECONDS), another.getDelay(NANOSECONDS));
        }

        @Override
        public void run() {
            worker.execute(task);
        }

    }


    /**
     * @see ExecutorService#shutdown()
     */
    public void shutdown() {
        scheduler.shutdown();
    }

    /**
     * @see ExecutorService#shutdownNow()
     */
    public void shutdownNow() {
        scheduler.shutdownNow();
    }

    /**
     * @see ExecutorService#isShutdown()
     */
    public boolean isShutdown() {
        return scheduler.isShutdown();
    }

    /**
     * @see ExecutorService#isTerminated()
     */
    public boolean isTerminated() {
        return scheduler.isTerminated();
    }

    /**
     * @see ExecutorService#awaitTermination(long, TimeUnit)
     */
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return scheduler.awaitTermination(timeout, unit);
    }

}
