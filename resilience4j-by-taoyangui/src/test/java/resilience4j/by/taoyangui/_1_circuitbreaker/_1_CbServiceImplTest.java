package resilience4j.by.taoyangui._1_circuitbreaker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.SCENARIO_1;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import resilience4j.by.taoyangui.v2.data.UserRepository;
import resilience4j.by.taoyangui.v2.data.UserResponse;
import resilience4j.by.taoyangui.v2.exception.IgnoredException;
import resilience4j.by.taoyangui.v2.exception.ProblemException;
import resilience4j.by.taoyangui.v2.service.business.BusinessService;
import resilience4j.by.taoyangui.v2.service.remote.RemoteService;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class _1_CbServiceImplTest {
  @Autowired
  @Qualifier("cbServiceImpl")
  BusinessService service;

  @MockBean RemoteService remote;
  @Autowired UserRepository repository;

  @Autowired CircuitBreakerRegistry cbRegistry;
  CircuitBreaker cb;

  @BeforeEach
  public void init() {
    cb = cbRegistry.circuitBreaker(SCENARIO_1);
  }

  @Order(1)
  @Test
  void closed_circuitbreaker_ignoring_IgnoredException() {
    // ringBufferSizeInClosedState = 5, failureRateThreshold = 20, but IgnoredException ignores
    Mockito.when(remote.process()).thenThrow(new IgnoredException("ignore IgnoredException"));
    for (int i = 0; i < 5; i++) {
      UserResponse response = service.businessProcess();
      assertEquals(State.CLOSED, response.getState());
    }
    Mockito.verify(remote, times(5)).process();
  }

  @Order(2)
  @Test
  void open_circuitbreaker_by_ProblemException() {
    // ringBufferSizeInClosedState = 5, failureRateThreshold = 20
    Mockito.when(remote.process()).thenThrow(new ProblemException("count ProblemException"));
    for (int i = 0; i < 4; i++) {
      UserResponse response = service.businessProcess();
      assertEquals(State.CLOSED, response.getState());
    }
    // open circuit breaker when hits 5 requests
    for (int i = 0; i < 2; i++) {
      UserResponse response = service.businessProcess();
      assertEquals(State.OPEN, response.getState());
    }
    // 6th time would not hits "remoteService.process()" because circuit breaker is opened
    Mockito.verify(remote, times(5)).process();
  }

  @SneakyThrows
  @Order(3)
  @Test
  void circuitbreaker_closed_open_half_open_closed_and_oepn_half_open_open() {
    // CLOSE
    // ringBufferSizeInClosedState = 5, failureRateThreshold = 20
    IgnoredException a = new IgnoredException("ignore IgnoredException");
    ProblemException b = new ProblemException("count ProblemException");
    Mockito.when(remote.process())
        .thenThrow(new IgnoredException[] {a, a}) // ignore, not even counted
        .thenReturn(repository.findAll())
        .thenReturn(repository.findAll())
        .thenThrow(new ProblemException[] {b, b, b}) // 3rd make it OPEN
        // wait and become HALF OPEN
        .thenThrow(a) // this one would not hit when it is OPEN, because block by circuit breaker
        .thenReturn(repository.findAll())
        .thenReturn(repository.findAll())
        .thenReturn(repository.findAll()) // CLOSE
        .thenThrow(b); // CLOSED -> OPEN -> HALF OPEN -> OPEN

    System.out.println("********** CLOSED -> OPEN -> HALF OPEN -> CLOSED **********");
    // will be ignored, so failed call is 0
    for (int i = 0; i < 2; i++) {
      UserResponse response = service.businessProcess();
      assertEquals(State.CLOSED, response.getState());
      assertEquals(-1.0, cb.getMetrics().getFailureRate());
      assertEquals(0, cb.getMetrics().getNumberOfFailedCalls());
    }
    for (int i = 0; i < 4; i++) {
      UserResponse response = service.businessProcess();
      assertEquals(State.CLOSED, response.getState());
      assertEquals(-1.0, cb.getMetrics().getFailureRate());
      assertEquals(Math.max(i - 1, 0), cb.getMetrics().getNumberOfFailedCalls());
    }
    System.out.println("********** OPEN **********");
    // open circuit breaker when hits 5 requests
    for (int i = 0; i < 2; i++) {
      UserResponse response = service.businessProcess();
      assertEquals(State.OPEN, response.getState());
      assertEquals(60.0, cb.getMetrics().getFailureRate());
      // 4th would not happen because opened circuit breaker
      assertEquals(3, cb.getMetrics().getNumberOfFailedCalls());
    }
    // 8th time would not hits "remoteService.process()" because circuit breaker is opened
    Mockito.verify(remote, times(7)).process();

    // OPEN -> HALF OPEN
    Thread.sleep(3000);

    // HALF OPEN
    System.out.println("********** HALF OPEN **********");
    assertEquals(State.HALF_OPEN, cb.getState());
    assertEquals(-1.0, cb.getMetrics().getFailureRate());
    assertEquals(0, cb.getMetrics().getNumberOfFailedCalls());

    for (int i = 0; i < 4; i++) {
      UserResponse response = service.businessProcess();
      assertEquals(i != 3 ? State.HALF_OPEN : State.CLOSED, response.getState());
    }
    System.out.println("********** CLOSED **********");

    // CLOSED -> OPEN -> HALF OPEN -> OPEN
    System.out.println("********** CLOSED -> OPEN -> HALF OPEN -> OPEN **********");
    for (int i = 0; i < 4; i++) {
      assertEquals(State.CLOSED, service.businessProcess().getState());
    }
    for (int i = 0; i < 2; i++) {
      assertEquals(State.OPEN, service.businessProcess().getState());
    }
    Thread.sleep(3000);
    for (int i = 0; i < 2; i++) {
      assertEquals(State.HALF_OPEN, service.businessProcess().getState());
    }
    for (int i = 0; i < 10; i++) {
      assertEquals(State.OPEN, service.businessProcess().getState());
    }
  }
}
