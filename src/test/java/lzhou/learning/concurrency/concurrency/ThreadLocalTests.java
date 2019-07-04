package lzhou.learning.concurrency.concurrency;

import org.junit.Assert;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description: ThreadLocal
 *   - When the thread is created by some framework, and thus parameters cannot be passed in in some other way (Connection Pool etc).
 *   - Avoid Synchornization blocks on some non-thread-safe objects (eg. SimpleDateFormat).
 *   - If the thread will not use the variable anymore, it must be removed (by calling `ThreadLocal.remove()`). Otherwise, it will cause memory leak.
 * @author: lingy
 * @Date: 2019-07-04 15:50:19
 * @param: null
 * @return:
 */
public class ThreadLocalTests {
    @Test
    public void testThreadLocalAsParam() throws InterruptedException {
        final int NUM_THREADS = 100;
        AtomicInteger curId = new AtomicInteger();
        ThreadLocal<Integer> threadId = ThreadLocal.withInitial(()->curId.getAndIncrement());

        Runnable printThreadId = () -> System.out.println(threadId.get());

        ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
        for (int i=0; i<NUM_THREADS; ++i) {
            executorService.execute(printThreadId);
        }
        executorService.shutdown();
        boolean normalExit = executorService.awaitTermination(10, TimeUnit.SECONDS);
        Assert.assertTrue(normalExit);
    }

    private static class ThreadSafeSimpleDateFormatUtil {
        private static ThreadLocal<SimpleDateFormat> formatter = ThreadLocal.withInitial(() -> new SimpleDateFormat());
        public static String format(Date date) {
            return formatter.get().format(date);
        }
    }

    @Test
    public void testSimpleDateFormatUtil() throws InterruptedException {
        final int NUM_THREADS = 100;
        Runnable printDate = () -> System.out.println(ThreadSafeSimpleDateFormatUtil.format(new Date()));

        ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
        for (int i=0; i<NUM_THREADS; ++i) {
            executorService.execute(printDate);
        }
        executorService.shutdown();
        boolean normalExit = executorService.awaitTermination(10, TimeUnit.SECONDS);
        Assert.assertTrue(normalExit);
    }
}
