package microservice.java.resilience4j;

import static java.lang.Math.min;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

// To with a bit of control to max threads running in the test.
public abstract class MultithreadingTestTools {
  // https://blog.csdn.net/j16421881/article/details/85471678
  public void submitToForkJoinPool(int concurrency, Runnable task) {
    // 默认会使用CPU数量减去1
    int defaultConcurrency = ForkJoinPool.commonPool().getParallelism();
    // 创建一个线程数为不大于(默认线程数的线程池+1)*2, 即2倍于CPU的线程
    ForkJoinPool forkJoinPool = new ForkJoinPool(min(concurrency, (defaultConcurrency + 1) * 2));
    try {
      forkJoinPool.submit(task).get();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }
}
