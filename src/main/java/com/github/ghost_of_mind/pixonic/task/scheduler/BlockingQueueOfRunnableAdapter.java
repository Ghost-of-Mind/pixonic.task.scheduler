package com.github.ghost_of_mind.pixonic.task.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.stream.Collectors.toCollection;


@RequiredArgsConstructor
class BlockingQueueOfRunnableAdapter<E extends Runnable> implements BlockingQueue<Runnable> {

    @Delegate(types = DelegatedMethods.class)
    private final BlockingQueue<E> queue;
    private final Function<Runnable, E> wrappingFunction;


    @Override
    public boolean add(final Runnable runnable) {
        return queue.add(wrappingFunction.apply(runnable));
    }

    @Override
    public boolean addAll(final Collection<? extends Runnable> c) {
        return queue.addAll(
            c.stream()                 // TODO: use Guava: use com.google.common.collect.Collections2#transform instead
                .map(wrappingFunction) // (not every collection can return its size for a constant time)
                .collect(toCollection(() -> new ArrayList<>(c.size())))
        );
    }

    @Override
    public void put(final Runnable runnable) throws InterruptedException {
        queue.put(wrappingFunction.apply(runnable));
    }

    @Override
    public boolean offer(final Runnable runnable) {
        return queue.offer(wrappingFunction.apply(runnable));
    }

    @Override
    public boolean offer(final Runnable runnable, final long timeout, final TimeUnit unit) throws InterruptedException {
        return queue.offer(wrappingFunction.apply(runnable), timeout, unit);
    }

    @Override
    public Iterator<Runnable> iterator() {
        return new DelegatingIterator<>(queue.iterator());
    }

    @RequiredArgsConstructor
    private final static class DelegatingIterator<E> implements Iterator<E> {
        @Delegate
        private final Iterator<? extends E> delegate;
    }


    interface DelegatedMethods {

        boolean removeAll(Collection<?> c);
        boolean retainAll(Collection<?> c);
        Runnable remove();
        void clear();
        Runnable poll();
        Runnable element();
        Runnable peek();
        Runnable take() throws InterruptedException;
        Runnable poll(long timeout, TimeUnit unit) throws InterruptedException;
        int remainingCapacity();
        boolean remove(Object o);
        boolean containsAll(Collection<?> c);
        int size();
        boolean isEmpty();
        boolean contains(Object o);
        Object[] toArray();
        <T> T[] toArray(T[] a);
        int drainTo(Collection<? super Runnable> c, int maxElements);
        int drainTo(Collection<? super Runnable> c);

    }

}
