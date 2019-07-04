package lzhou.learning.concurrency.concurrency;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @Description: The Dinning Philosopher Problem
 *   - Waiter Solution
 *   - Table Capacity Solution
 *   - Resource Hierarchy Solution
 *   - Chandy / Misra Solution
 *
 *   [哲学家就餐问题](https://baike.baidu.com/item/%E5%93%B2%E5%AD%A6%E5%AE%B6%E5%B0%B1%E9%A4%90%E9%97%AE%E9%A2%98/10929794)
 *
 *   [The Dinning Philosopher Problem](http://adit.io/posts/2013-05-11-The-Dining-Philosophers-Problem-With-Ron-Swanson.html)
 *
 * @author: lingy
 * @Date: 2019-07-03 11:00:30
 * @param: null
 * @return:
 */
public class DinningPhilosophersSolutionTests {
    /**
     * @Description: Waiter solution
     *   - A waiter is introduced, to manage the chopsticks.
     *   - Every philosopher must ask the waiter to get both forks.
     *   - The solution is suitable for small problems. Concurrency level is low.
     * @author: lingy
     * @Date: 2019-07-04 11:20:51
     * @param:
     * @return: void
     */
    @Test
    public void testWaiterSolution() throws InterruptedException {
        final int NUM_PHILOSOPHERS = 5;
        final int LOOP = 1000;

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
                int right = (ii + 1) % NUM_PHILOSOPHERS;
                for (int j=0; j<LOOP; ++j) {
                    waiter.lock();
                    System.out.println("Philosopher " + ii + ": Acquiring left [" + left + "] chopstick.");
                    chopsticks[left].lock();
                    System.out.println("Philosopher " + ii + ": Left chopstick [" + left + "] acquired.");

                    System.out.println("Philosopher " + ii + ": Acquiring right [" + right + "] chopstick.");
                    chopsticks[right].lock();
                    System.out.println("Philosopher " + ii + ": Right chopstick [" + right + "] acquired.");
                    waiter.unlock();

                    System.out.println("Philosopher " + ii + ": Do work.");
                    System.out.println("Philosopher " + ii + ": Done.");
                    chopsticks[left].unlock();
                    chopsticks[right].unlock();
                }
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

    /**
     * @Description: Semaphore Solution
     *   - Only four philosophers are allowed to be on the table.
     * @author: lingy
     * @Date: 2019-07-04 11:20:47
     * @param:
     * @return: void
     */
    @Test
    public void testSemaphoreSolution() throws InterruptedException {
        final int NUM_PHILOSOPHERS = 5;
        final int LOOP = 1000;

        Lock[] chopsticks = new ReentrantLock[NUM_PHILOSOPHERS];
        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            chopsticks[i] = new ReentrantLock();
        }
        Semaphore capacity = new Semaphore(NUM_PHILOSOPHERS-1);

        Runnable[] dinningPhilosophers = new Runnable[NUM_PHILOSOPHERS];

        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            int ii = i;
            dinningPhilosophers[i] = () -> {
                int left = ii;
                int right = (ii + 1) % NUM_PHILOSOPHERS;

                for (int j=0; j<LOOP; ++j) {
                    try {
                        capacity.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Philosopher " + ii + ": Acquiring first [" + left + "] chopstick.");
                    chopsticks[left].lock();
                    System.out.println("Philosopher " + ii + ": First chopstick [" + left + "] acquired.");
                    System.out.println("Philosopher " + ii + ": Acquiring second [" + right + "] chopstick.");
                    chopsticks[right].lock();
                    System.out.println("Philosopher " + ii + ": Second chopstick [" + right + "] acquired.");
                    System.out.println("Philosopher " + ii + ": Doing work.");
                    System.out.println("Philosopher " + ii + ": Done.");
                    chopsticks[left].unlock();
                    chopsticks[right].unlock();
                    capacity.release();
                }
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

    /**
     * @Description: Resource Hierarchy
     *   - Number all chopsticks.
     *   - Every philosopher must get the chopstick with lower number first, then the other chopstick.
     *   - The dependency graph is always acyclic, and deadlock will never happen.
     * @author: lingy
     * @Date: 2019-07-04 11:20:56
     * @param:
     * @return: void
     */
    @Test
    public void testResourceHierarchySolution() throws InterruptedException {
        final int NUM_PHILOSOPHERS = 5;
        final int LOOP = 1000;

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
                for (int j=0; j<LOOP; ++j) {
                    System.out.println("Philosopher " + ii + ": Acquiring first [" + first + "] chopstick.");
                    chopsticks[first].lock();
                    System.out.println("Philosopher " + ii + ": First chopstick [" + first + "] acquired.");
                    System.out.println("Philosopher " + ii + ": Acquiring second [" + second + "] chopstick.");
                    chopsticks[second].lock();
                    System.out.println("Philosopher " + ii + ": Second chopstick [" + second + "] acquired.");
                    System.out.println("Philosopher " + ii + ": Doing work.");
                    System.out.println("Philosopher " + ii + ": Done.");
                    chopsticks[first].unlock();
                    chopsticks[second].unlock();
                }
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
            if (owner==this.owner) {
                return;
            } if (isDirty) {
                this.owner = owner;
                isDirty = false;
            } else {
                try {
                    System.out.println("Philosopher "+owner+": Waiting for "+this.owner+"'s chopstick #"+number);
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        public synchronized void isDirty(boolean isDirty) {
            this.isDirty = isDirty;
            if (this.isDirty) {
                System.out.println("Philosopher "+owner+":Make dirty chopstick #"+number);
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
        final int LOOP = 10000;
        final int NUM_PHILOSOPHERS = 5;
        Chopstick[] chopsticks = new Chopstick[NUM_PHILOSOPHERS];
        for (int i=0; i<NUM_PHILOSOPHERS; ++i) {
            chopsticks[i] = new Chopstick(i, Math.min(i, NUM_PHILOSOPHERS-2));
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
        boolean normalExit = executorService.awaitTermination(10, TimeUnit.SECONDS);
        Assert.assertTrue(normalExit);
    }
}
