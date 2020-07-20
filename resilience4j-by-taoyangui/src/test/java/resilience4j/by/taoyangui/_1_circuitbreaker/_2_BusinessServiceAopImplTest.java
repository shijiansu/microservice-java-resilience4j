package resilience4j.by.taoyangui._1_circuitbreaker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static resilience4j.by.taoyangui.service.circuitbreaker.CircuitBreakerUtil.BACKEND_A;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
import resilience4j.by.taoyangui.exception.BusinessAException;
import resilience4j.by.taoyangui.exception.BusinessBException;
import resilience4j.by.taoyangui.remote_service.RemoteService;
import resilience4j.by.taoyangui.service.BusinessService;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class _2_BusinessServiceAopImplTest {
  @Autowired
  @Qualifier("businessServiceAopImpl")
  BusinessService businessService;

  @MockBean RemoteService remoteService;

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
}
