package lzhou.learning.concurrency.concurrency;

import org.junit.Assert;
import org.junit.Test;

/**
 * @Description: Thread, Runnable
 *
 * [Runnable和Thread比较](https://www.jianshu.com/p/9c9a11092f26)
 *
 * @author: lingy
 * @Date: 2019-06-27 14:34:41
 * @param: null
 * @return:
 */
public class JvmTaskTests {
    /**
     * @Description: 方法1: 继承Thread类, 并覆盖run()方法
     *   - 问题: Java只支持单继承
     * @author: lingy
     * @Date: 2019-06-27 11:55:30
     * @param: null
     * @return:
     */
    private static Thread[] makeThreads(int nthread, int sleep) {
        Thread[] threads = new Thread[nthread];
        for (int i=0; i<nthread; ++i) {
            int ii = i;
            threads[i] = new Thread() {
                public void run() {
                    System.out.println("Started, i=" + ii);
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        System.out.println("Interrupted, i=" + ii);
                    }
                    System.out.println("Ended, i=" + ii);
                }
            };
        }
        return threads;
    }

    @Test
    public void testExtendingThread() throws InterruptedException {
        final int nthread = 100;
        final int sleep = 1000;
        final long timeConsumedMax = 1200;
        long startTime = System.currentTimeMillis();
        Thread[] threads = makeThreads(nthread, sleep);
        for (int i=0; i<nthread; ++i) {
            threads[i].start();
        }
        for (int i=0; i<nthread; ++i) {
            // 注意, 下面一个竞争条件. 但我们忽略.
            threads[i].join(timeConsumedMax);
        }

        long endTime = System.currentTimeMillis();
        // 注意, 下面一个竞争条件. 但我们忽略.
        Assert.assertTrue(endTime - startTime < timeConsumedMax);
    }

    /**
     * @Description: 方法2: 实现Runnable接口, 并实现run()方法
     * @author: lingy
     * @Date: 2019-06-27 11:55:30
     * @param: null
     * @return:
     */
    private static Runnable[] makeRunnables(int nthread, int sleep) {
        Runnable[] runnables = new Runnable[nthread];
        for (int i=0; i<nthread; ++i) {
            int ii = i;
            runnables[i] = new Runnable() {
                @Override
                public void run() {
                    System.out.println("Started, i=" + ii);
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        System.out.println("Interrupted, i=" + ii);
                    }
                    System.out.println("Ended, i=" + ii);
                }
            };
        }
        return runnables;
    }

    @Test
    public void testImplementingRunnable() throws InterruptedException {
        final int nthread = 100;
        final int sleep = 1000;
        final long timeConsumedMax = 1200;
        long startTime = System.currentTimeMillis();
        Runnable[] runnables = makeRunnables(nthread, sleep);
        Thread[] threads = new Thread[nthread];
        for (int i=0; i<nthread; ++i) {
            threads[i] = new Thread(runnables[i]);
        }
        for (int i=0; i<nthread; ++i) {
            threads[i].start();
        }
        for (int i=0; i<nthread; ++i) {
            // 注意, 下面一个竞争条件. 但我们忽略.
            threads[i].join(timeConsumedMax);
        }

        long endTime = System.currentTimeMillis();
        // 注意, 下面一个竞争条件. 但我们忽略.
        Assert.assertTrue(endTime - startTime < timeConsumedMax);
    }
}
