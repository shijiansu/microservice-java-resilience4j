package microservice.java.resilience4j.retry;

import static microservice.java.resilience4j.test.BackendService.log;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.control.Try;
import java.util.function.Supplier;
import microservice.java.resilience4j.test.BackendService;
import microservice.java.resilience4j.test.MultithreadingTestTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class RetryTest extends MultithreadingTestTools {

  BackendService backendService;

  RetryConfig config;

  @BeforeEach
  public void init() {
    backendService = new BackendService();
    config = RetryConfig.custom().retryExceptions(RuntimeException.class).maxAttempts(2).build();
  }

  @Order(1)
  @Test
  public void retry_two_time() {
    Retry retry = Retry.of("retry_two_time", config);
    Supplier<String> decoratedSupplier =
        Retry.decorateSupplier(retry, backendService::doSomethingThrowException);

    // 第一次不会抛出异常, 被Retry捕获了. 第二次的时候, 会抛出异常, 被这个测试的Try,recover捕获
    String result =
        Try.ofSupplier(decoratedSupplier).recover(throwable -> "Hello from Recovery").get();
    log("TEST", result);
  }
}
