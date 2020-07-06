package microservice.java.resilience4j.blukhead;

import static microservice.java.resilience4j.test.BackendService.log;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
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

// 控制方法并行调用的次数
@TestMethodOrder(OrderAnnotation.class)
public class BulkHeadTest extends MultithreadingTestTools {

  BackendService backendService;

  BulkheadConfig config;

  @BeforeEach
  public void init() {
    backendService = new BackendService();
    config = BulkheadConfig.custom().maxConcurrentCalls(1).build();
  }

  @Order(1)
  @Test
  public void limit_one_thread_in_bulkhead() {
    Bulkhead bulkhead = Bulkhead.of("limit_one_thread_in_bulkhead", config);
    Supplier<String> decoratedSupplier =
        Bulkhead.decorateSupplier(bulkhead, backendService::doSomethingSlowly);

    submitToForkJoinPool(
        2,
        () ->
            IntStream.rangeClosed(1, 2)
                .parallel() // triggers 2 threads
                .forEach(
                    i -> {
                      log("TEST", "#" + i);
                      String result =
                          Try.ofSupplier(decoratedSupplier)
                              .recover(
                                  throwable -> throwable.getMessage() + " - Hello from Recovery")
                              .get();
                      log("TEST", "#" + i + " : " + result);
                    }));
  }
}
