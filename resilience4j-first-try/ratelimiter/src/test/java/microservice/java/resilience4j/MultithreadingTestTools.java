package microservice.java.resilience4j;

import static java.lang.Math.min;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

public abstract class MultithreadingTestTools {
  // https://blog.csdn.net/j16421881/article/details/85471678
  public void submitToForkJoinPool(int concurrency, Runnable task) {
    // 例如, 默认为4
    int defaultConcurrency = ForkJoinPool.commonPool().getParallelism();
    // 创建一个线程数为不大于默认线程数的线程池
    ForkJoinPool forkJoinPool = new ForkJoinPool(min(concurrency, defaultConcurrency));
    try {
      forkJoinPool.submit(task).get();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }
}
