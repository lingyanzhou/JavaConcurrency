package lzhou.learning.concurrency.concurrency;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description: 执行器 / 线程池
 *   - 优点:
 *     - 减少了每个任务调用的开销
 *     - 还维护着一些基本的统计数据
 *
 * ![ExecutorHierachy](img/ExecutorHierarchy.png)
 *
 * [java.util.concurrent 并发框架，异步执行器 Executor](https://blog.csdn.net/quan7394/article/details/58607968)
 *
 * [五种线程池的对比与使用](https://blog.csdn.net/qq_17045385/article/details/87883101)
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

    private static void assertInRange(long expectedMin, long expectedMax, long actual) {
        Assert.assertTrue(actual >= expectedMin);
        Assert.assertTrue(actual <= expectedMax);
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
        ExecutorService executor = Executors.newSingleThreadExecutor();
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

    /**
     * @Description: SingleThreadPool (单一工作线程线程池)
     *   - 创建一个大小为1的线程池, 超出的线程会在队列中等待
     *   - 适合需要保证所有任务按照指定顺序(FIFO, LIFO, 优先级)执行的场景
     *   - 潜在问题: 队列可能会无限增长, 造成JVM内存溢出
     *
     * @author: lingy
     * @Date: 2019-06-28 08:53:40
     * @param:
     * @return: void
     */
    @Test
    public void testSingleThreadPoolExecutor() throws InterruptedException, ExecutionException {
        final int ncallables = 4;
        final int sleep = 1000;
        final long minRuntime = 4000;
        final long maxRuntime = 4400;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicInteger atomicInteger = new AtomicInteger();
        Callable<Integer> callable = () -> {
            Thread.sleep(sleep);
            Integer i = atomicInteger.getAndIncrement();
            System.out.println("i = " + i);
            return i;
        };
        List<Callable<Integer>> callables = new ArrayList<>(ncallables);
        for (int i=0; i<ncallables; ++i) {
            callables.add(callable);
        }
        long startTime = System.currentTimeMillis();
        List<Future<Integer>> futures = executor.invokeAll(callables);
        long endTime = System.currentTimeMillis();
        long consumed = endTime - startTime;

        System.out.println("consumed = " + consumed);
        assertInRange(minRuntime, maxRuntime, consumed);
        for (int i=0; i<ncallables; ++i) {
            Assert.assertEquals(i, futures.get(i).get().longValue());
        }
    }

    /**
     * @Description: FixedThreadPool (固定大小线程池)
     *   - 创建一个定长线程池, 可控制线程最大并发数, 超出的线程会在队列中等待
     *   - 适合负载可估计的场景, 例如Web服务
     *   - 潜在问题: 队列可能会无限增长, 造成JVM内存溢出
     *
     * @author: lingy
     * @Date: 2019-06-28 08:53:40
     * @param:
     * @return: void
     */
    @Test
    public void testFixedThreadPoolExecutor() throws InterruptedException {
        final int nthreads = 2;
        final int ncallables = 4;
        final int sleep = 1000;
        final long minRuntime = 2000;
        final long maxRuntime = 2200;

        ExecutorService executor = Executors.newFixedThreadPool(nthreads);
        Callable<Integer> callable = () -> {
            Thread.sleep(sleep);
            return Integer.MAX_VALUE;
        };
        List<Callable<Integer>> callables = new ArrayList<>(ncallables);
        for (int i=0; i<ncallables; ++i) {
            callables.add(callable);
        }
        long startTime = System.currentTimeMillis();
        executor.invokeAll(callables);
        long endTime = System.currentTimeMillis();
        long consumed = endTime - startTime;

        System.out.println("consumed = " + consumed);
        assertInRange(minRuntime, maxRuntime, consumed);
    }

    /**
     * @Description: WorkStealingPool (工作窃取线程池)
     *   - 创建一个工作窃取程池
     *   - 为每个线程创建任务列表(双端队列), 减少线程间竞争
     *   - 允许线程窃取其他线程任务(从列表尾部)
     *   - 适合任务负载不均衡, 或递归创建子任务的场景.
     *   - 潜在问题: 消耗比FixedThreadPool更多的内存.
     * @author: lingy
     * @Date: 2019-06-28 08:53:40
     * @param:
     * @return: void
     */
    @Test
    public void testWorkStealingPoolExecutor() throws InterruptedException {
        final int nthreads = 2;
        final int ncallables = 4;
        final int sleep = 1000;
        final long minRuntime = 2000;
        final long maxRuntime = 2200;

        ExecutorService executor = Executors.newWorkStealingPool(nthreads);
        Callable<Integer> callable = () -> {
            Thread.sleep(sleep);
            return Integer.MAX_VALUE;
        };
        List<Callable<Integer>> callables = new ArrayList<>(ncallables);
        for (int i=0; i<ncallables; ++i) {
            callables.add(callable);
        }
        long startTime = System.currentTimeMillis();
        executor.invokeAll(callables);
        long endTime = System.currentTimeMillis();
        long consumed = endTime - startTime;

        System.out.println("consumed = " + consumed);
        assertInRange(minRuntime, maxRuntime, consumed);
    }

    /**
     * @Description: RecursiveTask 可递归的任务
     * @author: lingy
     * @Date: 2019-06-28 14:56:24
     * @param: null
     * @return: 
     */
    public static class FactorialTask extends RecursiveTask<Long> {
        private int start;
        private int end;

        public FactorialTask(int end) {
            this(1, end);
        }

        public FactorialTask(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            try {
                Thread.sleep(new Random().nextInt(1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (end==start) {
                return Long.valueOf(start);
            }
            int mid = (end+start-1) / 2;
            FactorialTask subTask1 = new FactorialTask(start, mid);
            FactorialTask subTask2 = new FactorialTask(mid+1, end);
            subTask1.fork();
            subTask2.fork();

            long result1 = subTask1.join();
            long result2 = subTask2.join();
            long result = result1 * result2;
            System.out.println("Thread = " + Thread.currentThread().getName() + ", result = " + result);
            return result;
        }
    }

    /**
     * @Description: ForkJoinTask (工作窃取线程池)
     *   - 使用WorkStealingPoolExecutor执行递归创建子任务
     */
    @Test
    public void testForkJoinTask() throws InterruptedException {
        final int nthreads = 40;
        final int factorial = 20;
        final long expected = 2432902008176640000L;

        ForkJoinPool executor = (ForkJoinPool)Executors.newWorkStealingPool(nthreads);
        ForkJoinTask<Long> forkJoinTask = executor.submit(new FactorialTask(factorial));
        Long result = forkJoinTask.join();
        System.out.println("result = "+result);
        executor.shutdown();

        Assert.assertEquals(expected, result.longValue());
    }

    /**
     * @Description: CachedThreadPool (无界线程池)
     *   - 创建一个可缓存线程池, 如果线程池长度超过处理需要, 可灵活回收空闲线程, 若无可回收, 则新建线程. 如果线程超过 60 秒还未被使用, 就会被中止并从缓存中移除.
     *   - 适合大量短生命周期, 负载较轻的异步任务
     *   - 潜在问题: 线程数量无限增加, 造成栈溢出的问题
     * @author: lingy
     * @Date: 2019-06-28 08:53:40
     * @param:
     * @return: void
     */
    @Test
    public void testCachedThreadPoolExecutor() throws InterruptedException {
        final int ncallables = 4;
        final int sleep = 1000;
        final long minRuntime = 1000;
        final long maxRuntime = 1100;

        ExecutorService executor = Executors.newCachedThreadPool();
        Callable<Integer> callable = () -> {
            Thread.sleep(sleep);
            return Integer.MAX_VALUE;
        };
        List<Callable<Integer>> callables = new ArrayList<>(ncallables);
        for (int i=0; i<ncallables; ++i) {
            callables.add(callable);
        }
        long startTime = System.currentTimeMillis();
        executor.invokeAll(callables);
        long endTime = System.currentTimeMillis();
        long consumed = endTime - startTime;

        System.out.println("consumed = " + consumed);
        assertInRange(minRuntime, maxRuntime, consumed);
    }

    /**
     * @Description: ScheduledThreadPool (调度线程池)
     *   - 创建一个可以延时启动，定时启动的调度线程池
     *
     *   - 适用于需要多个后台线程执行周期任务的场景
     *
     *   - 方法
     *     - ScheduledFuture ScheduledExecutorService.schedule(Runnable, long, TimeUnit)
     *
     *     - ScheduledFuture ScheduledExecutorService.schedule(Callable, long, TimeUnit)
     *
     *     - ScheduledFuture ScheduledExecutorService.scheduleAtFixedRate(Runnable, long initial_delay, long delay, TimeUnit)
     *
     *     - ScheduledFuture ScheduledExecutorService.scheduleWithFixedDelay(Runnable, long initial_delay, long delay, TimeUnit)
     *
     *   - 潜在问题: 线程数量无限增加, 造成栈溢出的问题
     *
     *   [深入理解Java线程池：ScheduledThreadPoolExecutor](https://www.jianshu.com/p/925dba9f5969)
     *
     * @author: lingy
     * @Date: 2019-06-28 08:53:40
     * @param:
     * @return: void
     */
    @Test
    public void testScheduledThreadPoolExecutor() throws InterruptedException, ExecutionException {
        final int nthreads= 2;
        final int sleep = 1000;

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(nthreads);
        Callable<Long> callable = () -> {
            System.out.println("In callable: thread = " + Thread.currentThread().getName() + "; time = " + System.currentTimeMillis());
            Thread.sleep(sleep);
            long time = System.currentTimeMillis();
            System.out.println("In callable: thread = " + Thread.currentThread().getName() + "; time = " + time);
            return time;
        };
        AtomicInteger atomicInteger = new AtomicInteger();
        Runnable runnable = () -> {
            try {
                Thread.sleep(996);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            atomicInteger.incrementAndGet();
            System.out.println("In Runnable: time = "+System.currentTimeMillis()+"; interation = "+atomicInteger.get());
        };
        AtomicInteger atomicInteger2 = new AtomicInteger();
        Runnable runnable2 = () -> {
            try {
                Thread.sleep(996);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            atomicInteger2.incrementAndGet();
            System.out.println("In Runnable 2: time = "+System.currentTimeMillis()+"; interation = "+atomicInteger2.get());
        };
        long startTime = System.currentTimeMillis();
        // Task 1 将从0ms到1000ms 执行
        ScheduledFuture<Long> future1 = executor.schedule(callable, 0, TimeUnit.MILLISECONDS);
        // Task 2 将从500ms到1500ms 执行
        ScheduledFuture<Long> future2 = executor.schedule(callable, sleep/2, TimeUnit.MILLISECONDS);
        // 由于500ms是没有可用线程, 将等待Task 1 退出. Task 3 将从1000ms到2000ms 执行
        ScheduledFuture<Long> future3 = executor.schedule(callable, sleep/2, TimeUnit.MILLISECONDS);
        // 周期性任务, 周期为1000ms. 周期间的任务互不影响
        executor.scheduleAtFixedRate(runnable, 2*sleep, sleep, TimeUnit.MILLISECONDS);
        // 周期性任务, 周期为1000ms+任务用时. 下一周期的任务等待上一周期的任务完成.
        executor.scheduleWithFixedDelay(runnable2, 2*sleep, sleep, TimeUnit.MILLISECONDS);

        Thread.sleep(55*sleep/10);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        Assert.assertEquals(4, atomicInteger.get());
        Assert.assertEquals(2, atomicInteger2.get());
        assertInRange(1000, 1100, future1.get()-startTime);
        assertInRange(1500, 1700, future2.get()-startTime);
        assertInRange(2000, 2200, future3.get()-startTime);
    }
}
