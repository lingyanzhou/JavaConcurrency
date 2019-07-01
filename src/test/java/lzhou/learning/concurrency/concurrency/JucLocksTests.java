package lzhou.learning.concurrency.concurrency;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class JucLocksTests {
    private long value = 0;

    @Before
    public void before() {
        value = 0;
    }

    @Test
    public void testMutex() throws InterruptedException {
        Semaphore mutex = new Semaphore(1);

        Runnable runnable = () -> {
            for (int i=0; i<10000; ++i) {
                try {
                    mutex.acquire();
                    try {
                        value++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        mutex.release();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i=0; i<10; ++i) {
            executorService.execute(runnable);
        }
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("Value = "+value);
        Assert.assertEquals(10*10000, value);
    }

    @Test
    public void testBufferWithSemaphore() throws InterruptedException {
        final int BUFF_SIZE = 10;
        Semaphore mutex = new Semaphore(1);
        Semaphore empty = new Semaphore(BUFF_SIZE);
        Semaphore filled = new Semaphore(BUFF_SIZE);
        filled.drainPermits();
        Queue<Integer> buffer = new LinkedList<>();
        Queue<Integer> result = new LinkedList<>();

        Runnable producer = () -> {
            for (int i=0; i<10000; ++i) {
                try {
                    empty.acquire();
                    mutex.acquire();
                    buffer.offer(i);
                    System.out.println("Produced : "+i);
                    mutex.release();
                    filled.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Runnable consumer = () -> {
            for (int i=0; i<10000; ++i) {
                try {
                    filled.acquire();
                    mutex.acquire();
                    int tmp = buffer.poll();
                    result.offer(tmp);
                    System.out.println("Consumed : "+tmp);
                    mutex.release();
                    empty.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(producer);
        executorService.execute(consumer);
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
        int size = result.size();
        for (int i=0; i<size; ++i) {
            long tmp = result.poll().longValue();
            Assert.assertEquals(i, tmp);
        }
    }
}
