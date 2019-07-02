package lzhou.learning.concurrency.concurrency;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    @Test
    public void testReadWriteLock() throws InterruptedException {
        final int READER_ENTER = 0;
        final int READER_EXIT = 1;
        final int WRITER_ENTER = 2;
        final int WRITER_EXIT = 3;

        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        Queue<Integer> readerWriterCallOrder = new LinkedList();
        Runnable reader = () -> {
            for (int i=0; i<1000; ++i) {
                readWriteLock.readLock().lock();
                synchronized (readerWriterCallOrder) {
                    readerWriterCallOrder.offer(READER_ENTER);
                }
                synchronized (readerWriterCallOrder) {
                    readerWriterCallOrder.offer(READER_EXIT);
                }
                readWriteLock.readLock().unlock();
            }
        };
        Runnable writer = () -> {
            for (int i=0; i<1000; ++i) {
                readWriteLock.writeLock().lock();
                synchronized (readerWriterCallOrder) {
                    readerWriterCallOrder.offer(WRITER_ENTER);
                }
                synchronized (readerWriterCallOrder) {
                    readerWriterCallOrder.offer(WRITER_EXIT);
                }
                readWriteLock.writeLock().unlock();
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(40);
        for (int i=0; i<20; ++i) {
            executorService.execute(writer);
        }
        for (int i=0; i<20; ++i) {
            executorService.execute(reader);
        }
        executorService.shutdown();
        boolean normalExit = executorService.awaitTermination(10, TimeUnit.SECONDS);
        Assert.assertTrue(normalExit);

        int readers = 0;
        int writers = 0;
        int maxReaders = 0;
        while (readerWriterCallOrder.size()>0) {
            int action = readerWriterCallOrder.poll();
            if (action==READER_ENTER) {
                Assert.assertEquals(0, writers);
                readers += 1;
            } else if (action==READER_EXIT) {
                Assert.assertEquals(0, writers);
                Assert.assertTrue(readers > 0);
                readers -= 1;
            } else if (action==WRITER_ENTER) {
                Assert.assertEquals(0, writers);
                Assert.assertEquals(0, readers);
                writers += 1;
            } else if (action==WRITER_EXIT) {
                Assert.assertEquals(1, writers);
                Assert.assertEquals(0, readers);
                writers -= 1;
            }
            maxReaders = Math.max(maxReaders, readers);
        }

        System.out.println("MaxReaders = " + maxReaders);
        Assert.assertTrue(maxReaders>=1);
    }
}
