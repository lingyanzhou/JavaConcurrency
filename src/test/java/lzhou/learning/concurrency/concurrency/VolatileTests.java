package lzhou.learning.concurrency.concurrency;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @Description: Volatile 关键字
 *   - 有序性: 禁止指令重排序优化
 *   - 可见性: 新值能立即同步到主内存，以及每次使用前立即从主内存刷新
 *   - 不保证原子性
 * @author: lingy
 * @Date: 2019-07-01 09:24:37
 * @param: null
 * @return:
 */
public class VolatileTests {
    private volatile int counter=0;

    @Before
    public void before() {
        counter = 0;
    }

    private static class AlternatingLock {
        private volatile boolean lock = false;

        public void lock(boolean side) {
            while (lock!=side) {
                //spin
            }
        }

        public void unlock(boolean side) {
            lock = !side;
        }
    }

    @Test
    public void testAlternatingLock() throws InterruptedException {
        final boolean EVEN = true;
        final boolean ODD = false;
        AlternatingLock lock = new AlternatingLock();
        // 让printEven先跑
        lock.unlock(ODD);

        List<Integer> orderList = new ArrayList<>();
        Runnable printOdd = () -> {
            for (int i=0; i<10; ++i) {
                lock.lock(ODD);
                System.out.println("ODD: i = "+(i*2+1));
                orderList.add(i*2+1);
                lock.unlock(ODD);
            }
        };
        Runnable printEven = () -> {
            for (int i=0; i<10; ++i) {
                lock.lock(EVEN);
                System.out.println("EVEN: i = "+i*2);
                orderList.add(i*2);
                lock.unlock(EVEN);
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        executorService.submit(printOdd);
        executorService.submit(printEven);

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);

        for (int i=0; i<orderList.size(); ++i) {
            Assert.assertEquals(i, orderList.get(i).longValue());
        }
    }

    @Test
    public void testVolatileIntIsNotAtomic() throws InterruptedException {
        Runnable runnable = () -> {
            for (int i=0; i<100000; ++i) {
                counter++;
            }
        };
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i=0; i<10; ++i) {
            executorService.submit(runnable);
        }
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println(counter);
        Assert.assertNotEquals(10*100000, counter);
    }

}
