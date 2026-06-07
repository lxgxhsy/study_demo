package com.example.concurrencylab.threadpool;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ResizableCapacityBlockingQueue<E> extends LinkedBlockingQueue<E> {

    private final ResizableSemaphore permits;
    private final ReentrantLock resizeLock = new ReentrantLock();
    private volatile int capacity;

    public ResizableCapacityBlockingQueue(int capacity) {
        super(Integer.MAX_VALUE);
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.permits = new ResizableSemaphore(capacity);
    }

    @Override
    public boolean offer(E e) {
        resizeLock.lock();
        try {
            if (!permits.tryAcquire()) {
                return false;
            }
            boolean offered = super.offer(e);
            if (!offered) {
                permits.release();
            }
            return offered;
        } finally {
            resizeLock.unlock();
        }
    }

    @Override
    public int remainingCapacity() {
        return Math.max(0, permits.availablePermits());
    }

    @Override
    public E take() throws InterruptedException {
        E value = super.take();
        permits.release();
        return value;
    }

    @Override
    public E poll() {
        E value = super.poll();
        if (value != null) {
            permits.release();
        }
        return value;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E value = super.poll(timeout, unit);
        if (value != null) {
            permits.release();
        }
        return value;
    }

    @Override
    public boolean remove(Object o) {
        boolean removed = super.remove(o);
        if (removed) {
            permits.release();
        }
        return removed;
    }

    @Override
    public void clear() {
        int size = size();
        super.clear();
        if (size > 0) {
            permits.release(size);
        }
    }

    public synchronized void setCapacity(int newCapacity) {
        resizeLock.lock();
        try {
            int currentSize = size();
            if (newCapacity < currentSize) {
                throw new IllegalArgumentException("new capacity is smaller than current queue size");
            }
            int delta = newCapacity - capacity;
            if (delta > 0) {
                permits.release(delta);
            } else if (delta < 0) {
                permits.reduce(-delta);
            }
            this.capacity = newCapacity;
        } finally {
            resizeLock.unlock();
        }
    }

    public int capacity() {
        return capacity;
    }

    private static class ResizableSemaphore extends Semaphore {
        ResizableSemaphore(int permits) {
            super(permits);
        }

        void reduce(int reduction) {
            reducePermits(reduction);
        }
    }
}
