package lzhou.learning.concurrency.concurrency;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @Description: 哲学家就餐问题
 *
 * [哲学家就餐问题](https://baike.baidu.com/item/%E5%93%B2%E5%AD%A6%E5%AE%B6%E5%B0%B1%E9%A4%90%E9%97%AE%E9%A2%98/10929794)
 *
 * @author: lingy
 * @Date: 2019-07-03 11:00:30
 * @param: null
 * @return:
 */
public class DinningPhilosophersSolutionTests {
    @Test
    public void testWaiterSolution() throws InterruptedException {
        final int NUM_PHILOSOPHERS = 5;

        Lock waiter = new ReentrantLock();
        Lock[] chopsticks = new ReentrantLock[NUM_PHILOSOPHERS];
        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            chopsticks[i] = new ReentrantLock();
        }

        Runnable[] dinningPhilosophers = new Runnable[NUM_PHILOSOPHERS];

        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            int ii = i;
            dinningPhilosophers[i] = () -> {
                int left = ii;

                waiter.lock();
                System.out.println("Philosopher " + ii + ": Acquiring left ["+left+"] chopstick.");
                chopsticks[left].lock();
                System.out.println("Philosopher " + ii + ": Left chopstick ["+left+"] acquired.");
                int right = (ii+1)%NUM_PHILOSOPHERS;
                System.out.println("Philosopher " + ii + ": Acquiring right ["+right+"] chopstick.");
                chopsticks[right].lock();
                System.out.println("Philosopher " + ii + ": Right chopstick ["+right+"] acquired.");
                waiter.unlock();

                System.out.println("Philosopher " + ii + ": Do work.");
                System.out.println("Philosopher " + ii + ": Done.");
                chopsticks[left].unlock();
                chopsticks[right].unlock();
            };
        }

        ExecutorService executorService = Executors.newFixedThreadPool(NUM_PHILOSOPHERS);

        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            executorService.execute(dinningPhilosophers[i]);
        }
        executorService.shutdown();
        boolean normalExit = executorService.awaitTermination(10, TimeUnit.SECONDS);
        Assert.assertTrue(normalExit);
    }

    @Test
    public void testLockOrderingSolution() throws InterruptedException {
        final int NUM_PHILOSOPHERS = 5;

        Lock[] chopsticks = new ReentrantLock[NUM_PHILOSOPHERS];
        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            chopsticks[i] = new ReentrantLock();
        }

        Runnable[] dinningPhilosophers = new Runnable[NUM_PHILOSOPHERS];

        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            int ii = i;
            dinningPhilosophers[i] = () -> {
                int first = Math.min(ii, (ii+1)%NUM_PHILOSOPHERS);
                int second = Math.max(ii, (ii+1)%NUM_PHILOSOPHERS);
                System.out.println("Philosopher " + ii + ": Acquiring first ["+first+"] chopstick.");
                chopsticks[first].lock();
                System.out.println("Philosopher " + ii + ": First chopstick ["+first+"] acquired.");
                System.out.println("Philosopher " + ii + ": Acquiring second ["+second+"] chopstick.");
                chopsticks[second].lock();
                System.out.println("Philosopher " + ii + ": Second chopstick ["+second+"] acquired.");
                System.out.println("Philosopher " + ii + ": Doing work.");
                System.out.println("Philosopher " + ii + ": Done.");
                chopsticks[first].unlock();
                chopsticks[second].unlock();
            };
        }

        ExecutorService executorService = Executors.newFixedThreadPool(NUM_PHILOSOPHERS);

        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            executorService.execute(dinningPhilosophers[i]);
        }
        executorService.shutdown();
        boolean normalExit = executorService.awaitTermination(10, TimeUnit.SECONDS);
        Assert.assertTrue(normalExit);
    }

    private static class Chopstick {
        private int number;
        private int owner;
        private boolean isDirty=true;
        public Chopstick(int number, int owner) {
            this.number = number;
            this.owner = owner;
            this.isDirty = true;
        }
        public synchronized void setOwner(int owner) {
            if (isDirty) {
                this.owner = owner;
                isDirty = false;
            } else {
                try {
                    System.out.println("Waiting for chopstick #"+number);
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        public synchronized void isDirty(boolean isDirty) {
            this.isDirty = isDirty;
            if (this.isDirty) {
                System.out.println("Make dirty chopstick #"+number);
                this.notifyAll();
            }
        }
    }

    /**
     * @Description: Chandy / Misra solution
     *
     * [The dinning philosophers problem](https://www.eecis.udel.edu/~cshen/361/Notes/chandy.pdf)
     *
     * @author: lingy
     * @Date: 2019-07-03 18:15:34
     * @param:
     * @return: void
     */
    @Test
    public void testChandyMisraSolution() throws InterruptedException {
        final int LOOP = 1000;
        final int NUM_PHILOSOPHERS = 5;
        Chopstick[] chopsticks = new Chopstick[NUM_PHILOSOPHERS];
        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            chopsticks[i] = new Chopstick(i, Math.min(i, (i+1)%NUM_PHILOSOPHERS));
        }
        Runnable[] dinningPhilosophers = new Runnable[NUM_PHILOSOPHERS];
        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            int ii = i;
            dinningPhilosophers[i] = () -> {
                Chopstick leftChopstick = chopsticks[ii];
                Chopstick rightChopstick = chopsticks[(ii+1)%NUM_PHILOSOPHERS];

                for (int j=0; j<LOOP; ++j) {
                    System.out.println("Philosopher "+ii+": Thinking.");
                    System.out.println("Philosopher "+ii+": Requesting chopstick #"+leftChopstick.number);
                    leftChopstick.setOwner(ii);
                    System.out.println("Philosopher "+ii+": Requesting chopstick #"+rightChopstick.number);
                    rightChopstick.setOwner(ii);
                    System.out.println("Philosopher "+ii+":Doing work.");
                    System.out.println("Philosopher "+ii+":Done.");
                    leftChopstick.isDirty(true);
                    rightChopstick.isDirty(true);
                }
            };
        }

        ExecutorService executorService = Executors.newFixedThreadPool(NUM_PHILOSOPHERS);

        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            executorService.execute(dinningPhilosophers[i]);
        }
        executorService.shutdown();
        boolean normalExit = executorService.awaitTermination(50, TimeUnit.SECONDS);
        Assert.assertTrue(normalExit);
    }
}
