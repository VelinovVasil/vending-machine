package com.vendingmachine.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class VendingMachineStateCoordinatorTest {

    @Test
    void allowsConcurrentReaders() {
        VendingMachineStateCoordinator coordinator = new VendingMachineStateCoordinator();
        CountDownLatch readersEntered = new CountDownLatch(2);
        CountDownLatch releaseReaders = new CountDownLatch(1);

        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                List<Future<Integer>> readers = List.of(
                        executor.submit(() -> coordinator.read(() -> awaitRelease(readersEntered, releaseReaders))),
                        executor.submit(() -> coordinator.read(() -> awaitRelease(readersEntered, releaseReaders))));

                readersEntered.await();
                releaseReaders.countDown();
                assertThat(readers.get(0).get()).isEqualTo(1);
                assertThat(readers.get(1).get()).isEqualTo(1);
            }
        });
    }

    private int awaitRelease(CountDownLatch readersEntered, CountDownLatch releaseReaders) {
        readersEntered.countDown();
        try {
            releaseReaders.await();
            return 1;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Reader was interrupted", exception);
        }
    }
}
