package microservice.java.resilience4j.timelimiter;

import static microservice.java.resilience4j.test.BackendService.log;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.vavr.control.Try;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import microservice.java.resilience4j.test.BackendService;
import microservice.java.resilience4j.test.MultithreadingTestTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class TimeLimiterTest extends MultithreadingTestTools {

  BackendService backendService;

  TimeLimiterConfig config;

  @BeforeEach
  public void init() {
    backendService = new BackendService();
    config =
        TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(600)) // 超过600ms就会跑出超时异常
            .cancelRunningFuture(true) // .cancelRunningFuture(false)
            .build();
  }

  @Order(1)
  @Test
  public void timeout() {
    TimeLimiter timeLimiter = TimeLimiter.of(config);

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Supplier<Future<String>> futureSupplier =
        () -> executorService.submit(backendService::doSomethingSlowly);

    Callable<String> restrictedCall =
        TimeLimiter.decorateFutureSupplier(timeLimiter, futureSupplier);
    Try.of(restrictedCall::call).onFailure(throwable -> log("TEST", "We might have timed out"));
  }
}
