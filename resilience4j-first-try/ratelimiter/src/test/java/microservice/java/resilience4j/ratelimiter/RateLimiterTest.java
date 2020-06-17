package microservice.java.resilience4j.ratelimiter;

import static microservice.java.resilience4j.ratelimiter.BackendService.log;
import static microservice.java.resilience4j.ratelimiter.BackendService.sleep;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.vavr.control.Try;
import java.time.Duration;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import microservice.java.resilience4j.MultithreadingTestTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class RateLimiterTest extends MultithreadingTestTools {

  BackendService backendService;

  RateLimiterConfig config;

  @BeforeEach
  public void init() {
    backendService = new BackendService();
    config =
        RateLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(100)) // thread waits for a permission
            .limitRefreshPeriod(Duration.ofSeconds(1)) // period of a limit refresh
            .limitForPeriod(1) // number of permissions available during refresh period
            .build();
  }

  // [15-Jun-2020 00:02:35:430] - [ForkJoinPool-1-worker-3] - TEST - #3
  // [15-Jun-2020 00:02:35:428] - [ForkJoinPool-1-worker-1] - TEST - #2
  // [15-Jun-2020 00:02:35:465] - [ForkJoinPool-1-worker-3] - TEST - #3 : true // 线程3先进入, 成功调用后台服务
  // [15-Jun-2020 00:02:35:565] - [ForkJoinPool-1-worker-1] - TEST - #2 : false // 1秒内只接受1个请求, 该线程失败
  // [15-Jun-2020 00:02:35:970] - [ForkJoinPool-1-worker-3] - TEST - #5
  // [15-Jun-2020 00:02:36:070] - [ForkJoinPool-1-worker-1] - TEST - #1
  // [15-Jun-2020 00:02:36:071] - [ForkJoinPool-1-worker-3] - TEST - #5 : false // 500ms后, 还是在1秒以内
  // [15-Jun-2020 00:02:36:173] - [ForkJoinPool-1-worker-1] - TEST - #1 : false // 500ms后, 还是在1秒以内
  // [15-Jun-2020 00:02:36:573] - [ForkJoinPool-1-worker-3] - TEST - #4
  // [15-Jun-2020 00:02:36:574] - [ForkJoinPool-1-worker-3] - TEST - #4 : true // 超过1秒, 成功处理
  @Order(1)
  @Test
  public void limit_one_in_refresh_period_with_parallel() {
    RateLimiter rateLimiter = RateLimiter.of("limit_one_in_refresh_period_with_parallel", config);
    Supplier<String> restrictedSupplier =
        RateLimiter.decorateSupplier(rateLimiter, backendService::doSomething);

    submitToForkJoinPool(
        2,
        () ->
            IntStream.rangeClosed(1, 5)
                .parallel()
                .forEach(
                    i -> {
                      log("TEST", "#" + i);
                      Try<String> aTry = Try.ofSupplier(restrictedSupplier);
                      log("TEST", "#" + i + " : " + aTry.isSuccess());
                      sleep(500);
                    }));
  }

  // [15-Jun-2020 00:03:26:438] - [ForkJoinPool-1-worker-3] - TEST - #1
  // [15-Jun-2020 00:03:26:461] - [ForkJoinPool-1-worker-3] - TEST - #1 : true
  // [15-Jun-2020 00:03:26:962] - [ForkJoinPool-1-worker-3] - TEST - #2
  // [15-Jun-2020 00:03:27:064] - [ForkJoinPool-1-worker-3] - TEST - #2 : false // 等500ms, 仍在1秒内
  // [15-Jun-2020 00:03:27:565] - [ForkJoinPool-1-worker-3] - TEST - #3
  // [15-Jun-2020 00:03:27:565] - [ForkJoinPool-1-worker-3] - TEST - #3 : true // 等500ms, 已经过了1秒
  // [15-Jun-2020 00:03:28:069] - [ForkJoinPool-1-worker-3] - TEST - #4
  // [15-Jun-2020 00:03:28:175] - [ForkJoinPool-1-worker-3] - TEST - #4 : false // 同理
  // [15-Jun-2020 00:03:28:679] - [ForkJoinPool-1-worker-3] - TEST - #5
  // [15-Jun-2020 00:03:28:679] - [ForkJoinPool-1-worker-3] - TEST - #5 : true // 同理
  @Order(2)
  @Test
  public void limit_one_in_refresh_period_with_sequential() {
    RateLimiter rateLimiter = RateLimiter.of("limit_one_in_refresh_period2", config);
    Supplier<String> restrictedSupplier =
        RateLimiter.decorateSupplier(rateLimiter, backendService::doSomething);

    IntStream.rangeClosed(1, 5)
        .sequential() // sequential so thread pool only take 1 thread to process
        .forEach(
            i -> {
              log("TEST", "#" + i);
              Try<String> aTry = Try.ofSupplier(restrictedSupplier);
              log("TEST", "#" + i + " : " + aTry.isSuccess());
              if (i % 2 == 1) {
                assertTrue(aTry.isSuccess());
              } else {
                assertFalse(aTry.isSuccess());
              }
              sleep(500);
            });
  }

  // [15-Jun-2020 00:09:49:492] - [ForkJoinPool-1-worker-3] - TEST - #1
  // [15-Jun-2020 00:09:49:515] - [ForkJoinPool-1-worker-3] - TEST - #1 : true
  // [15-Jun-2020 00:09:50:419] - [ForkJoinPool-1-worker-3] - TEST - #2
  // 下1个周期, 是00:09:49:492+1秒, 是00:09:50:492.
  // 00:09:50:419加上timeoutDuration的100ms是可以等到拿下一个permission的时候的, 所以为true
  // [15-Jun-2020 00:09:50:471] - [ForkJoinPool-1-worker-3] - TEST - #2 : true // 同理
  // [15-Jun-2020 00:09:51:372] - [ForkJoinPool-1-worker-3] - TEST - #3
  // [15-Jun-2020 00:09:51:475] - [ForkJoinPool-1-worker-3] - TEST - #3 : true // 同理
  // [15-Jun-2020 00:09:52:375] - [ForkJoinPool-1-worker-3] - TEST - #4
  // [15-Jun-2020 00:09:52:474] - [ForkJoinPool-1-worker-3] - TEST - #4 : true // 同理
  // [15-Jun-2020 00:09:53:375] - [ForkJoinPool-1-worker-3] - TEST - #5
  // [15-Jun-2020 00:09:53:473] - [ForkJoinPool-1-worker-3] - TEST - #5 : true // 同理
  @Order(3)
  @Test
  // To test "timeoutDuration", wait how long (timeout) to get next permission
  public void timeout_duration() {
    RateLimiter rateLimiter = RateLimiter.of("timeout_duration", config);
    Supplier<String> restrictedSupplier =
        RateLimiter.decorateSupplier(rateLimiter, backendService::doSomething);

    submitToForkJoinPool(
        1,
        () ->
            IntStream.rangeClosed(1, 5)
                .sequential()
                .forEach(
                    i -> {
                      log("TEST", "#" + i);
                      Try<String> aTry = Try.ofSupplier(restrictedSupplier);
                      log("TEST", "#" + i + " : " + aTry.isSuccess());
                      assertTrue(aTry.isSuccess()); // additional code, also take time to process
                      sleep(900);
                    }));
  }

  // [18-Jun-2020 00:05:22:134] - [ForkJoinPool-1-worker-3] - TEST - #1
  // [18-Jun-2020 00:05:22:158] - [ForkJoinPool-1-worker-3] - TEST - #1 : true
  // [18-Jun-2020 00:05:23:013] - [ForkJoinPool-1-worker-3] - TEST - #2
  // [18-Jun-2020 00:05:23:113] - [ForkJoinPool-1-worker-3] - TEST - #2 : true
  // 分析:
  // "timeoutDuration"的作用, 在RateLimiter中, 获得下次周期剩余的时间.
  // 如果少于"timeoutDuration", 则在线程中等待至下次周期来临时唤醒线程去处理请求.
  // 上述打印的日志, 第1个请求00:05:22:134, 看起来下一次的周期是00:05:23:134.
  // 但为什么第2个请求00:05:23:013仍可以成功(加上100ms的timeoutDuration仍在1秒周期内)?
  // 原因在于源码处理时候仍需要时间, 所以到Java代码中, 其时间戳已经超过1秒的周期时间, 所以可以处理成功.
  // 要得到稳定的测试结果, 可将下面的sleep(850);调到sleep(700);
  // 源代码:
  // AtomicRateLimiter.acquirePermission;
  // AtomicRateLimiter.waitForPermissionIfNecessary;
  @Order(4)
  @Test
  public void timeout_duration_boundary_testing() {
    RateLimiter rateLimiter = RateLimiter.of("timeout_duration_boundary_testing", config);
    // Decorate
    Supplier<String> restrictedSupplier =
        RateLimiter.decorateSupplier(rateLimiter, backendService::doSomething);

    submitToForkJoinPool(
        1,
        () ->
            IntStream.rangeClosed(1, 2)
                .sequential()
                .forEach(
                    i -> {
                      log("TEST", "#" + i);
                      Try<String> aTry = Try.ofSupplier(restrictedSupplier);
                      log("TEST", "#" + i + " : " + aTry.isSuccess());
                      sleep(850);
                    }));
  }

  @Order(5)
  @Test
  public void timeout_duration_false_as_limit_one() {
    RateLimiter rateLimiter = RateLimiter.of("timeout_duration_false_as_limit_one", config);
    // Decorate
    Supplier<String> restrictedSupplier =
        RateLimiter.decorateSupplier(rateLimiter, backendService::doSomething);

    submitToForkJoinPool(
        1,
        () ->
            IntStream.rangeClosed(1, 2)
                .sequential()
                .forEach(
                    i -> {
                      log("TEST", "#" + i);
                      Try<String> aTry = Try.ofSupplier(restrictedSupplier);
                      log("TEST", "#" + i + " : " + aTry.isSuccess());
                      if (1 == i) {
                        assertTrue(aTry.isSuccess());
                      } else if (i == 2) {
                        assertFalse(aTry.isSuccess());
                      }
                      sleep(700);
                    }));
  }
}
