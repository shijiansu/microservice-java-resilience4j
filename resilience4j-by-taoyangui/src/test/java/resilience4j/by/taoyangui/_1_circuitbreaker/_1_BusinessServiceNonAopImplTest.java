package resilience4j.by.taoyangui._1_circuitbreaker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static resilience4j.by.taoyangui.service.circuitbreaker.CircuitBreakerUtil.BACKEND_A;

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
import resilience4j.by.taoyangui.data.Response;
import resilience4j.by.taoyangui.data.UserRepository;
import resilience4j.by.taoyangui.exception.BusinessAException;
import resilience4j.by.taoyangui.exception.BusinessBException;
import resilience4j.by.taoyangui.remote_service.RemoteService;
import resilience4j.by.taoyangui.service.BusinessService;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class _1_BusinessServiceNonAopImplTest {
  @Autowired
  @Qualifier("businessServiceNonAopImpl")
  BusinessService businessService;

  @MockBean RemoteService remoteService;
  @Autowired UserRepository repository;

  @Autowired CircuitBreakerRegistry cbRegistry;
  CircuitBreaker cb;

  @BeforeEach
  public void init() {
    cb = cbRegistry.circuitBreaker(BACKEND_A);
  }

  @Order(1)
  @Test
  void closed_circuitbreaker_ignoring_BusinessAException() {
    // ringBufferSizeInClosedState = 5, failureRateThreshold = 20, but BusinessAException ignores
    Mockito.when(remoteService.process()).thenThrow(new BusinessAException("ignore A exception"));
    for (int i = 0; i < 5; i++) {
      Response response = businessService.businessProcess();
      assertEquals(State.CLOSED, response.getState());
    }
    Mockito.verify(remoteService, times(5)).process();
  }

  @Order(2)
  @Test
  void open_circuitbreaker_by_BusinessBException() {
    // ringBufferSizeInClosedState = 5, failureRateThreshold = 20
    Mockito.when(remoteService.process()).thenThrow(new BusinessBException("count B exception"));
    for (int i = 0; i < 4; i++) {
      Response response = businessService.businessProcess();
      assertEquals(State.CLOSED, response.getState());
    }
    // open circuit breaker when hits 5 requests
    for (int i = 0; i < 2; i++) {
      Response response = businessService.businessProcess();
      assertEquals(State.OPEN, response.getState());
    }
    // 6th time would not hits "remoteService.process()" because circuit breaker is opened
    Mockito.verify(remoteService, times(5)).process();
  }

  @SneakyThrows
  @Order(3)
  @Test
  void circuitbreaker_closed_open_half_open_closed_and_oepn_half_open_open() {
    // CLOSE
    // ringBufferSizeInClosedState = 5, failureRateThreshold = 20
    BusinessAException a = new BusinessAException("ignore A exception");
    BusinessBException b = new BusinessBException("count B exception");
    Mockito.when(remoteService.process())
        .thenThrow(new BusinessAException[] {a, a}) // ignore, not even counted
        .thenReturn(repository.findAll())
        .thenReturn(repository.findAll())
        .thenThrow(new BusinessBException[] {b, b, b}) // 3rd make it OPEN
        // wait and become HALF OPEN
        .thenThrow(a) // this one would not hit when it is OPEN, because block by circuit breaker
        .thenReturn(repository.findAll())
        .thenReturn(repository.findAll())
        .thenReturn(repository.findAll()) // CLOSE
        .thenThrow(b); // CLOSED -> OPEN -> HALF OPEN -> OPEN

    System.out.println("********** CLOSED -> OPEN -> HALF OPEN -> CLOSED **********");
    // will be ignored, so failed call is 0
    for (int i = 0; i < 2; i++) {
      Response response = businessService.businessProcess();
      assertEquals(State.CLOSED, response.getState());
      assertEquals(-1.0, cb.getMetrics().getFailureRate());
      assertEquals(0, cb.getMetrics().getNumberOfFailedCalls());
    }
    for (int i = 0; i < 4; i++) {
      Response response = businessService.businessProcess();
      assertEquals(State.CLOSED, response.getState());
      assertEquals(-1.0, cb.getMetrics().getFailureRate());
      assertEquals(Math.max(i - 1, 0), cb.getMetrics().getNumberOfFailedCalls());
    }
    System.out.println("********** OPEN **********");
    // open circuit breaker when hits 5 requests
    for (int i = 0; i < 2; i++) {
      Response response = businessService.businessProcess();
      assertEquals(State.OPEN, response.getState());
      assertEquals(60.0, cb.getMetrics().getFailureRate());
      // 4th would not happen because opened circuit breaker
      assertEquals(3, cb.getMetrics().getNumberOfFailedCalls());
    }
    // 8th time would not hits "remoteService.process()" because circuit breaker is opened
    Mockito.verify(remoteService, times(7)).process();

    // OPEN -> HALF OPEN
    Thread.sleep(3000);

    // HALF OPEN
    System.out.println("********** HALF OPEN **********");
    assertEquals(State.HALF_OPEN, cb.getState());
    assertEquals(-1.0, cb.getMetrics().getFailureRate());
    assertEquals(0, cb.getMetrics().getNumberOfFailedCalls());

    for (int i = 0; i < 4; i++) {
      Response response = businessService.businessProcess();
      assertEquals(i != 3 ? State.HALF_OPEN : State.CLOSED, response.getState());
    }
    System.out.println("********** CLOSED **********");

    // CLOSED -> OPEN -> HALF OPEN -> OPEN
    System.out.println("********** CLOSED -> OPEN -> HALF OPEN -> OPEN **********");
    for (int i = 0; i < 4; i++) {
      assertEquals(State.CLOSED, businessService.businessProcess().getState());
    }
    for (int i = 0; i < 2; i++) {
      assertEquals(State.OPEN, businessService.businessProcess().getState());
    }
    Thread.sleep(3000);
    for (int i = 0; i < 2; i++) {
      assertEquals(State.HALF_OPEN, businessService.businessProcess().getState());
    }
    for (int i = 0; i < 10; i++) {
      assertEquals(State.OPEN, businessService.businessProcess().getState());
    }
  }
}
