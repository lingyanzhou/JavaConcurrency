package lzhou.learning.concurrency.concurrency;

import lombok.Getter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
}
