package microservice.java.resilience4j.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.vavr.control.Try;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import microservice.java.resilience4j.test.BackendService;
import microservice.java.resilience4j.test.MultithreadingTestTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class CircuitBreakerTest extends MultithreadingTestTools {

  BackendService backendService;

  CircuitBreakerConfig config;

  @BeforeEach
  public void init() {
    backendService = new BackendService();
    config = CircuitBreakerConfig.custom().failureRateThreshold(20).build();
  }

  @Order(1)
  @Test
  public void register() {
    CircuitBreaker circuitBreaker = CircuitBreaker.of("register", config);

    Supplier<String> restrictedSupplier =
        CircuitBreaker.decorateSupplier(circuitBreaker, backendService::doSomethingThrowException);

    submitToForkJoinPool(
        2,
        () ->
            IntStream.rangeClosed(1, 50)
                .parallel()
                .forEach(i -> Try.ofSupplier(restrictedSupplier)));
  }
}
