package lzhou.learning.concurrency.concurrency;

import lombok.Getter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @Description: synchronized 块
 *   - 偏向锁: JVM比较锁对象头里的thread id信息
 *   - 轻量级锁: 等待的线程自旋, 而不是阻塞
 *   - 重量级锁: 等待的线程被阻塞
 *
 * @author: lingy
 * @Date: 2019-07-01 10:49:57
 * @param: null
 * @return:
 */
public class SynchronizedBlockTests {
    public static class ConcurrentCounter {
        private static int curId = 0;
        @Getter
        private int id = 0;
        @Getter
        private int count = 0;

        private ConcurrentCounter(int id) {
            this.id = id;
            count = 0;
        }

        /**
         * @Description: synchronized block.
         *   - 所修饰的block为critical section
         *   - 进入block须要获得括号内对象的锁
         *   - 出block时自动释放锁
         * @author: lingy
         * @Date: 2019-06-27 09:55:40
         * @param: step
         * @return: void
         */
        public void incr(int step) {
            synchronized (this) {
                count += step;
            }
        }
        
        /**
         * @Description: synchronized method.
         *   - 等效于把整个方法放入一个synchronized (this) {}
         *   - 进入方法须要获得调用对象的锁
         *   - synchronized 是可重入的
         * @author: lingy
         * @Date: 2019-06-27 09:55:44
         * @param: 
         * @return: void
         */
        synchronized public void incr1() {
            this.incr(1);
        }

        /**
         * @Description: synchronized static method.
         *   - 等效于把整个方法放入一个synchronized (some.class) {}
         *   - 进入方法须要获得类对象的锁
         * @author: lingy
         * @Date: 2019-06-27 09:55:44
         * @param:
         * @return: void
         */
        synchronized static public ConcurrentCounter getInstance() {
            ConcurrentCounter cnt = new ConcurrentCounter(curId);
            curId += 1;
            return cnt;
        }

        synchronized static public int getCurId() {
            return curId;
        }
        synchronized static public void resetCurId() {
            curId = 0;
        }
    }

    @Before
    public void before() {
        ConcurrentCounter.resetCurId();
    }

    @After
    public void after() {
        ConcurrentCounter.resetCurId();
    }

    @Test
    public void testSynchronizedBlock() throws InterruptedException {
        final int expected = 100;
        final int nthreads = 20;

        ExecutorService executorService = Executors.newFixedThreadPool(nthreads);
        ConcurrentCounter concurrentCounter = ConcurrentCounter.getInstance();

        for (int i=0; i<expected; ++i) {
            int ii = i;
            executorService.execute(() -> {
                concurrentCounter.incr(1);
                // 注意, ii 和 count 不一定一致, 因为Runnable不一定按顺序执行
                System.out.println("i = "+ ii + "; count = " + concurrentCounter.getCount());
            });
        }
        executorService.shutdown();
        // 注意, 下面一个竞争条件. 但我们忽略.
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        Assert.assertEquals(expected, concurrentCounter.getCount());
    }

    @Test
    public void testSynchronizedMethod() throws InterruptedException {
        final int expected = 100;
        final int nthreads = 20;

        ExecutorService executorService = Executors.newFixedThreadPool(nthreads);
        ConcurrentCounter concurrentCounter = ConcurrentCounter.getInstance();

        for (int i=0; i<expected; ++i) {
            int ii = i;
            executorService.execute(() -> {
                concurrentCounter.incr1();
                // 注意, ii 和 count 不一定一致, 因为Runnable不一定按顺序执行
                System.out.println("i = "+ ii + "; count = " + concurrentCounter.getCount());
            });
        }
        executorService.shutdown();
        // 注意, 下面一个竞争条件. 但我们忽略.
        executorService.awaitTermination( 10, TimeUnit.SECONDS);

        Assert.assertEquals(expected, concurrentCounter.getCount());
    }

    @Test
    public void testSynchronizedStaticMethod() throws InterruptedException {
        final int expected = 100;
        final int nthreads = 20;

        ExecutorService executorService = Executors.newFixedThreadPool(nthreads);

        for (int i=0; i<expected; ++i) {
            int ii = i;
            executorService.execute(() -> {
                ConcurrentCounter.getInstance();
                // 注意, ii 和 curid 不一定一致, 因为Runnable不一定按顺序执行
                System.out.println("i = "+ ii + "; curid = " + ConcurrentCounter.getCurId());
            });
        }
        executorService.shutdown();
        // 注意, 下面一个竞争条件. 但我们忽略.
        executorService.awaitTermination( 10, TimeUnit.SECONDS);

        Assert.assertEquals(expected, ConcurrentCounter.getCurId());
    }

    @Test
    public void testSynchronizedMethodDiffObjects() throws InterruptedException {
        final int expected = 20;
        final int expected2 = 20;
        final int nthreads = 20;

        ExecutorService executorService = Executors.newFixedThreadPool(nthreads);
        ConcurrentCounter concurrentCounter[] = new ConcurrentCounter[expected];

        for (int i=0; i<expected; ++i) {
            int ii = i;
            executorService.execute(() -> {
                concurrentCounter[ii] = ConcurrentCounter.getInstance();
                // 注意, ii 和 id 不一定一致, 因为Runnable不一定按顺序执行
                System.out.println("i = "+ ii + "; id= " + concurrentCounter[ii].getId());
                for (int j=0; j<expected2; ++j) {
                    int jj = j;
                    concurrentCounter[ii].incr1();
                    // 注意, ii 和 id, jj 和 counter 不一定一致, 因为Runnable不一定按顺序执行
                    System.out.println("i = "+ ii + "; id= " + concurrentCounter[ii].getId()+"; j = "+ jj + "; count= " + concurrentCounter[ii].getCount());
                }
            });
        }
        executorService.shutdown();
        // 注意, 下面一个竞争条件. 但我们忽略.
        executorService.awaitTermination( 10, TimeUnit.SECONDS);
        Assert.assertEquals(expected, ConcurrentCounter.getCurId());
        for (int i=0; i<expected; ++i) {
            Assert.assertEquals(expected2, concurrentCounter[i].getCount());
        }
    }

    /**
     * @Description: 简单非重入锁, 使用sync块实现
     *   - wait: 在该对象的synchronized 同步代码块里使用, 使当前线程阻塞，前提是必须先获得锁
     *   - notify: 在该对象的synchronized 同步代码块里使用, 只唤醒一个等待的线程
     *   - notifyAll: 在该对象的synchronized 同步代码块里使用, 唤醒所有等待的线程
     * @author: lingy
     * @Date: 2019-07-01 14:41:32
     */
    private static class SimpleLock {
        @Getter
        private Object lock = new Object();
        private boolean locked = false;
        public void acquire() {
            synchronized (lock) {
                while (locked) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                locked = true;
            }
        }

        public void release() {
            synchronized (lock) {
                locked = false;
                lock.notifyAll();
            }
        }
    }

    @Test
    public void testSimpleLock() throws InterruptedException {
        SimpleLock simpleLock = new SimpleLock();
        Queue<String> result = new LinkedList<>();
        Runnable runnable = () -> {
            for (int i=0; i<100; ++i) {
                simpleLock.acquire();
                result.offer(Thread.currentThread().getName());
                try {
                    Thread.sleep(new Random().nextInt(10));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                result.offer(Thread.currentThread().getName());
                simpleLock.release();
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i=0; i<10; ++i) {
            executorService.execute(runnable);
        }

        executorService.shutdown();
        boolean normalTerm = executorService.awaitTermination(100, TimeUnit.SECONDS);
        Assert.assertTrue(normalTerm);
        Assert.assertTrue(result.size() % 2 == 0);
        final int pairs = result.size() / 2;
        for (int i=0; i<pairs; ++i) {
            Assert.assertEquals(result.poll(), result.poll());
        }
    }

    /**
     * @Description: 简单条件变量, 使用sync块实现
     * @author: lingy
     * @Date: 2019-07-01 14:41:32
     */
    private static class SimpleCondition {
        private SimpleLock lock;

        public SimpleCondition(SimpleLock lock) {
            this.lock = lock;
        }

        public void await() {
            synchronized (lock.lock) {
                lock.release();
                boolean notified = false;
                while (!notified) {
                    try {
                        lock.lock.wait();
                        notified = true;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                lock.acquire();
            }
        }

        public void signalAll() {
            synchronized (lock.lock) {
                lock.lock.notifyAll();
            }
        }
    }

    @Test
    public void testSimpleCondition() throws InterruptedException {
        SimpleLock simpleLock = new SimpleLock();
        SimpleCondition filledCondition = new SimpleCondition(simpleLock);
        SimpleCondition emptyCondition = new SimpleCondition(simpleLock);
        Queue<Long> buff = new LinkedList<>();
        Queue<Long> result = new LinkedList<>();
        final int BUFF_SIZE = 10;
        Runnable producer = () -> {
            for (int i=0; i<1000; ++i) {
                simpleLock.acquire();
                while (buff.size()==BUFF_SIZE) {
                    System.out.println("Producer: Waiting");
                    filledCondition.await();
                }
                buff.offer((long) i);
                System.out.println("Producer: Value = "+i);
                emptyCondition.signalAll();
                simpleLock.release();
            }
        };
        Runnable consumer = () -> {
            for (int i=0; i<1000; ++i) {
                simpleLock.acquire();
                while (buff.size()==0) {
                    System.out.println("Producer: Waiting");
                    emptyCondition.await();
                }
                System.out.println(buff.size());
                long tmp = buff.poll();
                result.offer(tmp);
                System.out.println("Consumer: Value = "+tmp);
                filledCondition.signalAll();
                simpleLock.release();
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        executorService.execute(producer);
        executorService.execute(consumer);
        executorService.shutdown();
        boolean normalExit = executorService.awaitTermination(2, TimeUnit.SECONDS);
        Assert.assertTrue(normalExit);
        final int resultSize = result.size();
        for (int i=0; i<resultSize; ++i) {
            Assert.assertEquals(i, result.poll().longValue());
        }
    }

    /**
     * @Description: 简单信号量, 以sync block, wait/notify实现
     * @author: lingy
     * @Date: 2019-07-01 14:41:32
     */
    private static class SimpleSemaphore {
        private SimpleLock lock = new SimpleLock();
        private int permits;

        public SimpleSemaphore(int permits) {
            this.permits = permits;
        }

        public void acquire() {
            synchronized (lock.lock) {
                while (this.permits == 0) {
                    try {
                        lock.lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                this.permits -= 1;
            }
        }

        public void release() {
            synchronized (lock.lock) {
                this.permits += 1;
                lock.lock.notifyAll();
            }
        }

        public int acquireAll() {
            synchronized (lock.lock) {
                int tmp = this.permits;
                this.permits = 0;
                return tmp;
            }
        }
    }

    @Test
    public void testSimpleSemaphore() throws InterruptedException {
        final int BUFF_SIZE = 10;
        SimpleSemaphore mutex = new SimpleSemaphore(1);
        SimpleSemaphore filledSemaphore = new SimpleSemaphore(BUFF_SIZE);
        filledSemaphore.acquireAll();
        SimpleSemaphore emptyCondition = new SimpleSemaphore(BUFF_SIZE);
        Queue<Long> buff = new LinkedList<>();
        Queue<Long> result = new LinkedList<>();
        Runnable producer = () -> {
            for (int i=0; i<1000; ++i) {
                emptyCondition.acquire();
                mutex.acquire();
                buff.offer((long) i);
                System.out.println("Producer: Value = "+i);
                filledSemaphore.release();
                mutex.release();
            }
        };
        Runnable consumer = () -> {
            for (int i=0; i<1000; ++i) {
                filledSemaphore.acquire();
                mutex.acquire();
                long tmp = buff.poll();
                result.offer(tmp);
                System.out.println("Consumer: Value = "+tmp);
                emptyCondition.release();
                mutex.release();
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        executorService.execute(producer);
        executorService.execute(consumer);

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
        final int resultSize = result.size();
        for (int i=0; i<resultSize; ++i) {
            Assert.assertEquals(i, result.poll().longValue());
        }
    }
}
