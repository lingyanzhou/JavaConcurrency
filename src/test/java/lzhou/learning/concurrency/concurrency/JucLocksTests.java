package lzhou.learning.concurrency.concurrency;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

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

        boolean normalExit = executorService.awaitTermination(10, TimeUnit.SECONDS);
        Assert.assertTrue(normalExit);

        System.out.println("Value = "+value);
        Assert.assertEquals(10*10000, value);
    }

    @Test
    public void testProducerConsumerWithSemaphore() throws InterruptedException {
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
                    filled.release();
                    mutex.release();
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
                    empty.release();
                    mutex.release();
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

    @Test
    public void testProducerConsumerWithCondition() throws InterruptedException {
        final int BUFF_SIZE = 10;
        Lock lock = new ReentrantLock();
        Condition filledCondition = lock.newCondition();
        Condition emptyCondition = lock.newCondition();
        Queue<Long> buffer = new LinkedList<>();
        Queue<Long> result = new LinkedList<>();

        Runnable producer = () -> {
            for (int i=0; i<10000; ++i) {
                lock.lock();
                while (buffer.size()==BUFF_SIZE) {
                    try {
                        filledCondition.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                buffer.offer((long)i);
                System.out.println("Produced : "+i);
                emptyCondition.signalAll();
                lock.unlock();
            }
        };

        Runnable consumer = () -> {
            for (int i=0; i<10000; ++i) {
                lock.lock();
                while (buffer.size()==0) {
                    try {
                        emptyCondition.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                long tmp = buffer.poll();
                result.offer(tmp);
                System.out.println("Consumer : "+tmp);
                filledCondition.signalAll();
                lock.unlock();
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(producer);
        executorService.execute(consumer);
        executorService.shutdown();
        boolean normalExit = executorService.awaitTermination(10, TimeUnit.SECONDS);
        Assert.assertTrue(normalExit);
        int size = result.size();
        for (int i=0; i<size; ++i) {
            long tmp = result.poll().longValue();
            Assert.assertEquals(i, tmp);
        }
    }

    private static void latchAwait(CountDownLatch countDownLatch) {
        boolean awaitDone = false;
        while (!awaitDone) {
            try {
                countDownLatch.await();
                awaitDone = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testDeadLockWithCountDownLatch() throws InterruptedException {
        final int NUM_PHILOSOPHERS = 5;
        CountDownLatch step1Latch = new CountDownLatch(NUM_PHILOSOPHERS);
        CountDownLatch step2Latch = new CountDownLatch(NUM_PHILOSOPHERS);
        CountDownLatch exitLatch = new CountDownLatch(NUM_PHILOSOPHERS);

        Lock[] chopsticks = new ReentrantLock[NUM_PHILOSOPHERS];
        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            chopsticks[i] = new ReentrantLock();
        }

        Runnable[] dinningPhilosophers = new Runnable[NUM_PHILOSOPHERS];

        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            int ii = i;
            dinningPhilosophers[i] = () -> {
                int left = ii;
                step1Latch.countDown();
                latchAwait(step1Latch);
                System.out.println("Philosopher " + ii + ": Acquiring left ["+left+"] chopstick.");
                chopsticks[left].lock();
                System.out.println("Philosopher " + ii + ": Left chopstick ["+left+"] acquired.");

                step2Latch.countDown();
                latchAwait(step2Latch);
                int right = (ii+1)%NUM_PHILOSOPHERS;
                System.out.println("Philosopher " + ii + ": Acquiring right ["+right+"] chopstick.");
                chopsticks[right].lock();
                System.out.println("Philosopher " + ii + ": Right chopstick ["+right+"] acquired.");
                System.out.println("Philosopher " + ii + ": Do work.");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Philosopher " + ii + ": Done.");
                chopsticks[left].unlock();
                chopsticks[right].unlock();
                exitLatch.countDown();
            };
        }

        ExecutorService executorService = Executors.newFixedThreadPool(NUM_PHILOSOPHERS);

        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            executorService.execute(dinningPhilosophers[i]);
        }
        boolean normalExit = exitLatch.await(10, TimeUnit.SECONDS);
        Assert.assertFalse(normalExit);
        Assert.assertEquals(NUM_PHILOSOPHERS, exitLatch.getCount());
    }

    @Test
    public void testLiveLockWithCountDownLatch() throws InterruptedException {
        final int NUM_PHILOSOPHERS = 5;
        final int NUM_TRIES = 100;
        CountDownLatch[] acquireLeftLatches = new CountDownLatch[NUM_TRIES];
        for (int i=0; i<NUM_TRIES; ++i) {
            acquireLeftLatches[i] = new CountDownLatch(NUM_PHILOSOPHERS);
        }
        CountDownLatch[] acquireRightLatches = new CountDownLatch[NUM_TRIES];
        for (int i=0; i<NUM_TRIES; ++i) {
            acquireRightLatches[i] = new CountDownLatch(NUM_PHILOSOPHERS);
        }
        CountDownLatch[] releaseLeftLatches = new CountDownLatch[NUM_TRIES];
        for (int i=0; i<NUM_TRIES; ++i) {
            releaseLeftLatches[i] = new CountDownLatch(NUM_PHILOSOPHERS);
        }
        CountDownLatch[] continueLatches = new CountDownLatch[NUM_TRIES];
        for (int i=0; i<NUM_TRIES; ++i) {
            continueLatches[i] = new CountDownLatch(NUM_PHILOSOPHERS);
        }
        CountDownLatch exitLatch = new CountDownLatch(NUM_PHILOSOPHERS);
        CountDownLatch giveUpLatch = new CountDownLatch(NUM_PHILOSOPHERS);

        Lock[] chopsticks = new ReentrantLock[NUM_PHILOSOPHERS];
        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            chopsticks[i] = new ReentrantLock();
        }

        Runnable[] dinningPhilosophers = new Runnable[NUM_PHILOSOPHERS];

        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            int ii = i;
            dinningPhilosophers[i] = () -> {
                int left = ii;
                int right = (ii+1)%NUM_PHILOSOPHERS;

                boolean bothAcquired = false;
                int tries = 0;
                while (!bothAcquired) {
                    System.out.println("Philosopher " + ii + ": Tries = "+ tries);
                    acquireLeftLatches[tries].countDown();
                    latchAwait(acquireLeftLatches[tries]);
                    System.out.println("Philosopher " + ii + ": Acquiring left [" + left + "] chopstick.");
                    chopsticks[left].lock();
                    System.out.println("Philosopher " + ii + ": Left chopstick [" + left + "] acquired.");

                    acquireRightLatches[tries].countDown();
                    latchAwait(acquireRightLatches[tries]);

                    System.out.println("Philosopher " + ii + ": Acquiring right [" + right + "] chopstick.");
                    boolean rightAcquired = chopsticks[right].tryLock();

                    releaseLeftLatches[tries].countDown();
                    latchAwait(releaseLeftLatches[tries]);
                    if (!rightAcquired) {
                        System.out.println("Philosopher " + ii + ": Releasing left [" + left + "] chopstick.");
                        chopsticks[left].unlock();
                        continueLatches[tries].countDown();
                        latchAwait(continueLatches[tries]);
                        tries += 1;
                        if (tries == NUM_TRIES) {
                            System.out.println("Philosopher " + ii + ": Giving up.");
                            giveUpLatch.countDown();
                            return;
                        }
                        continue;
                    } else {
                        System.out.println("Philosopher " + ii + ": Right chopstick [" + right + "] acquired.");
                        bothAcquired = true;
                    }
                }

                System.out.println("Philosopher " + ii + ": Do work.");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Philosopher " + ii + ": Done.");
                chopsticks[left].unlock();
                chopsticks[right].unlock();
                exitLatch.countDown();
            };
        }

        ExecutorService executorService = Executors.newFixedThreadPool(NUM_PHILOSOPHERS);

        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            executorService.execute(dinningPhilosophers[i]);
        }
        boolean normalExit = giveUpLatch.await(5, TimeUnit.SECONDS);
        Assert.assertTrue(normalExit);
        Assert.assertEquals(NUM_PHILOSOPHERS, exitLatch.getCount());
        Assert.assertEquals(0, giveUpLatch.getCount());
    }
}
