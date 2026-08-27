package com.vendingmachine.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

@Component
public class VendingMachineStateCoordinator {

    private final ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();

    public <T> T read(Supplier<T> operation) {
        return execute(stateLock.readLock(), operation);
    }

    public <T> T write(Supplier<T> operation) {
        return execute(stateLock.writeLock(), operation);
    }

    public void write(Runnable operation) {
        write(() -> {
            operation.run();
            return null;
        });
    }

    private <T> T execute(Lock lock, Supplier<T> operation) {
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }
}
