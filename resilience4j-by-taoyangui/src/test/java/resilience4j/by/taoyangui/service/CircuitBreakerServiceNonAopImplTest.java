package resilience4j.by.taoyangui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static resilience4j.by.taoyangui.CircuitBreakerUtil.BACKEND_A;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import resilience4j.by.taoyangui.data.Response;
import resilience4j.by.taoyangui.remote_service.RemoteServiceImpl;

@SpringBootTest
public class CircuitBreakerServiceNonAopImplTest {
  @Autowired CircuitBreakerRegistry registry;

  @Autowired CircuitBreakerServiceNonAopImpl circuitService;

  @SneakyThrows
  @Test
  public void circuitBreakerTest() {
    for (int i = 0; i < 10; i++) {
      Response response = circuitService.circuitBreak();
      if (i < 6) {
        assertEquals(State.CLOSED, response.getState());
      } else {
        // i == 6, total 7 requests
        // ignore BusinessAException (2 BusinessAException);
        // ringBufferSizeInClosedState: 5, after 5 then calculate the failure rate (2 succ, 3
        // BusinessBException)
        assertEquals(State.OPEN, response.getState());
      }
    }
    assertEquals(
        7, RemoteServiceImpl.COUNT.get()); // because circuit breaker and no more processing

    Thread.sleep(Duration.ofMillis(3000).toMillis()); // wait for half open

    CircuitBreaker cb = registry.circuitBreaker(BACKEND_A);
    assertEquals(
        State.HALF_OPEN,
        cb.getState()); // because automaticTransitionFromOpenToHalfOpenEnabled: true

    System.out.println("***********************************");

    for (int i = 0; i < 10; i++) {
      Response response = circuitService.circuitBreak();
    }
  }
}
