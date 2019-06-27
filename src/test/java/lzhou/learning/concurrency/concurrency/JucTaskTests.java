package lzhou.learning.concurrency.concurrency;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.*;

/**
 * @Description: Java并发框架 java.util.concurrent
 *
 * [Java并发编程：Callable、Future和FutureTask](https://www.cnblogs.com/dolphin0520/p/3949310.html)
 *
 * @author: lingy
 * @Date: 2019-06-27 12:24:51
 * @param: null
 * @return:
 */
public class JucTaskTests {
    /**
     * @Description: Executor 异步执行框架, 封装了任务提交和执行过程, 支持多种不同类型的任务执行策略
     * Runnable 封装了线程所执行的操作, 该操作没有返回值 (可视为返回`Callable<null>`)
     * Callable 封装了线程所执行的操作, 该操作有返回值
     * Future 是Runnable和Callable的调度容器
     * @author: lingy
     * @Date: 2019-06-27 13:09:13
     * @param:
     * @return: void
     */
    @Test
    public void testExecutorRunnableFuture() throws InterruptedException, ExecutionException {
        final int nthread = 100;
        final int sleep = 1000;
        final long timeConsumedMax = 1200;
        long startTime = System.currentTimeMillis();

        Runnable runnables[] = new Runnable[nthread];
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

        ExecutorService executorService = Executors.newFixedThreadPool(nthread);

        Future futures[] = new Future[nthread];
        for (int i=0; i<nthread; ++i) {
            futures[i] = executorService.submit(runnables[i]);
        }
        for (int i=0; i<nthread; ++i) {
            Object o  = futures[i].get(); // Future.get() 阻塞, 知道线程结束或中断
            Assert.assertNull(o);
        }

        long endTime = System.currentTimeMillis();
        // 注意, 下面一个竞争条件. 但我们忽略.
        Assert.assertTrue(endTime - startTime < timeConsumedMax);
    }

    @Test
    public void testExecutorCallableFuture() throws InterruptedException, ExecutionException {
        final int nthread = 100;
        final int sleep = 1000;
        final long timeConsumedMax = 1200;
        long startTime = System.currentTimeMillis();

        Callable<Integer> calllables[] = new Callable[nthread];
        for (int i=0; i<nthread; ++i) {
            int ii = i;
            calllables[i] = new Callable() {
                @Override
                public Integer call() {
                    System.out.println("Started, i=" + ii);
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        System.out.println("Interrupted, i=" + ii);
                    }
                    System.out.println("Ended, i=" + ii);
                    return ii;
                }
            };
        }

        ExecutorService executorService = Executors.newFixedThreadPool(nthread);

        Future<Integer> futures[] = new Future[nthread];
        for (int i=0; i<nthread; ++i) {
            futures[i] = executorService.submit(calllables[i]);
        }
        for (int i=0; i<nthread; ++i) {
            Integer result  = futures[i].get(); // Future.get() 阻塞, 知道线程结束或中断
            Assert.assertEquals((long)i, result.longValue());
        }

        long endTime = System.currentTimeMillis();
        // 注意, 下面一个竞争条件. 但我们忽略.
        Assert.assertTrue(endTime - startTime < timeConsumedMax);
    }

    /**
     * @Description: FutureTask 实现了RunnableFuture, 间接实现Runnable和Future. 既可以提交给ExecuteService或Thread执行, 又可以通过get()取得结果
     * 相比Callable, Runnable, FutureTask确保任务只执行一次
     *
     * ![FutureTaskHierarchy](img/FutureTaskHierarchy.png)
     *
     * [Java进阶之FutureTask的用法及解析](https://blog.csdn.net/chenliguan/article/details/54345993)
     *
     * @author: lingy
     * @Date: 2019-06-27 13:49:51
     * @param: null
     * @return:
     */
    @Test
    public void testFutureTask() throws ExecutionException, InterruptedException {
        Integer expected = ThreadLocalRandom.current().nextInt();
        FutureTask<Integer> futureTask = new FutureTask(()->{
            Integer result = expected;
            System.out.println("In thread, result = " + result);
            return result;
        });

        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(futureTask);

        Integer result = futureTask.get();

        Assert.assertEquals(expected, result);
        Assert.assertTrue(futureTask.isDone());
        Assert.assertFalse(futureTask.isCancelled());
    }

    /**
     * @Description: FutureTask 实现了RunnableFuture, 间接实现Runnable和Future. 既可以提交给ExecuteService或Thread执行, 又可以通过get()取得结果
     * @author: lingy
     * @Date: 2019-06-27 13:49:51
     * @param: null
     * @return:
     */

    @Test()
    public void testFutureTaskCancelling() throws ExecutionException, InterruptedException {
        final int sleep = 10000;

        Integer expected = ThreadLocalRandom.current().nextInt();
        FutureTask<Integer> futureTask = new FutureTask(()->{
            System.out.println("Starting...");
            Integer result = expected;
            System.out.println("In thread, result = " + result);
            Thread.sleep(sleep);
            System.out.println("Ending...");
            return result;
        });

        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(futureTask);

        futureTask.cancel(true);
        try {
            futureTask.get();
        } catch (Exception e) {
            Assert.assertNotNull((CancellationException)e);
        }
        Assert.assertTrue(futureTask.isDone());
        Assert.assertTrue(futureTask.isCancelled());
    }
}
