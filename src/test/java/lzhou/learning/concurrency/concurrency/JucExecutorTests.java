package lzhou.learning.concurrency.concurrency;

import org.junit.Assert;
import org.junit.Test;
import sun.nio.ch.ThreadPool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description: 执行器 / 线程池
 *
 * ![ExecutorHierachy](img/ExecutorHierarchy.png)
 *
 * [java.util.concurrent 并发框架，异步执行器 Executor](https://blog.csdn.net/quan7394/article/details/58607968)
 *
 * @author: lingy
 * @Date: 2019-06-27 15:14:44
 * @param: null
 * @return:
 */
public class JucExecutorTests {
    private static void assertThrows(Class clazz, Callable callable) {
        try {
            callable.call();
        } catch (Throwable e) {
            Assert.assertEquals(clazz, e.getClass());
        }
    }

    /**
     * @Description: ExecutorService 常用方法.
     *   - void ExecutorService.execute(Runnable)
     *   提交任务, 不等待
     *   - Future ExecutorService.submit(Runnable)
     *   提交任务, 不等待, 返回Future
     *   - Future ExecutorService.submit(Callable)
     *   提交任务, 不等待, 返回Future
     *   - T ExecutorService.invokeAny(Collection)
     *   提交,执行所有Callable, 等待任意一个正常返回, 中断其他线程, 返回该值. 若无任何线程返回则为null.
     *   - List<Future> ExecutorService.invokeAll(Collection)
     *   提交, 执行, 等待所有Callable, 返回Future. 返回时所有Future为isDone(正常或非正常退出).
     *   - void ExecutorService.shutdown()
     *   停止接受新任务, 终止等待列表里未执行的任务, 等待正在执行的任务完成
     *   - void ExecutorService.shutdownNow()
     *   停止接受新任务, 终止等待列表里未执行的任务, 中断正在执行的任务完成
     *   - boolean ExecutorService.awaitTermination(int, TimeUnit)
     *   等待所有线程结束执行
     *   - boolean ExecutorService.isShutDown()
     *   执行shutdown命令后, 状态变为isShutDown. 可能还有线程在执行
     *   - boolean ExecutorService.isTerminated()
     *   所有线程结束执行, 状态未isTerminated
     * @author: lingy
     * @Date: 2019-06-27 16:08:40
     * @param: 
     * @return: void
     */
    @Test
    public void testExecutorOperations() throws ExecutionException, InterruptedException {
        final int nthread=10;
        final int expectedFinalVal = 6;

        AtomicInteger atomicInteger = new AtomicInteger();
        Runnable runnable = () -> atomicInteger.incrementAndGet();
        Callable<Integer> callable = () -> atomicInteger.incrementAndGet();
        Callable<Integer> callableSameReturn = () -> {
            atomicInteger.incrementAndGet();
            return Integer.MAX_VALUE;
        };
        Callable<Integer> callableErr = () -> Integer.valueOf("abc");

        // 从Executors工厂类获取ExecutorService实例
        ExecutorService executor = Executors.newFixedThreadPool(nthread);
        //提交Runnable, 无返回
        executor.execute(runnable); // 1
        //提交Runnable, 返回Future
        Future<?> tmp1 = executor.submit(runnable); // 2
        //提交Callable, 返回Future
        Future<Integer> tmp2 = executor.submit(callable); // 3
        //提交,执行所有Callable, 等待任意一个正常返回, 中断其他线程, 返回该值. 若无任何线程返回则为null.
        Integer result1 = executor.invokeAny(Arrays.asList(callableSameReturn, callableErr, callableErr)); // 4
        //提交, 执行, 等待所有Callable, 返回Future. 返回时所有Future为isDone(正常或非正常退出).
        List<Future<Integer>> futures = executor.invokeAll(Arrays.asList(callableSameReturn, callableSameReturn, callableErr)); // 6

        Assert.assertFalse(executor.isShutdown());
        Assert.assertFalse(executor.isTerminated());

        executor.shutdown();
        Assert.assertTrue(executor.isShutdown()); // 此时executor.isTerminated() 不一定为true

        // 试图正常退出, 若超时, 则强制退出.
        if (executor.awaitTermination(10, TimeUnit.SECONDS)) {
            System.out.println("正常退出");
        } else {
            executor.shutdownNow();
            System.out.println("强制退出");
        }
        Assert.assertTrue(executor.isTerminated());
        Assert.assertEquals(expectedFinalVal, atomicInteger.get());

        Assert.assertEquals(Integer.MAX_VALUE, result1.longValue());
        for (Future f: futures) {
            Assert.assertTrue(f.isDone());
        }
        Assert.assertEquals(Integer.MAX_VALUE, futures.get(0).get().longValue());
        Assert.assertEquals(Integer.MAX_VALUE, futures.get(1).get().longValue());
        assertThrows(ExecutionException.class, ()->futures.get(2).get());
    }

    @Test
    public void testFixedThreadPoolExecutor() throws InterruptedException {
        int nthreads = 2;
        int ncallables = 4;
        long minRuntime = 2000;
        long maxRuntime = 2500;

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nthreads);
        Callable<Integer> callable = () -> {
            Thread.sleep(1000);
            return Integer.MAX_VALUE;
        };
        List<Callable<Integer>> callables = new ArrayList<>(ncallables);
        for (int i=0; i<ncallables; ++i) {
            callables.add(callable);
        }
        long startTime = System.currentTimeMillis();
        executor.invokeAll(callables);
        long endTime = System.currentTimeMillis();

        Assert.assertTrue(endTime - startTime < maxRuntime);
        Assert.assertTrue(endTime - startTime > minRuntime);
    }
}
