package resilience4j.by.taoyangui._1_circuitbreaker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.SCENARIO_1;

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
import resilience4j.by.taoyangui.v2.data.UserResponse;
import resilience4j.by.taoyangui.v2.exception.IgnoredException;
import resilience4j.by.taoyangui.v2.exception.ProblemException;
import resilience4j.by.taoyangui.v2.service.business.BusinessService;
import resilience4j.by.taoyangui.v2.service.remote.RemoteService;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class _2_CbServiceAopImplTest {
  @Autowired
  @Qualifier("cbServiceAopImpl")
  BusinessService business;
  @MockBean RemoteService remote;

  @Autowired CircuitBreakerRegistry cbRegistry;
  CircuitBreaker cb;

  @BeforeEach
  public void init() {
    cb = cbRegistry.circuitBreaker(SCENARIO_1);
  }

  @Order(1)
  @Test
  void closed_circuitbreaker_ignoring_IgnoredException() {
    // ringBufferSizeInClosedState = 5, failureRateThreshold = 20, but BusinessAException ignores
    Mockito.when(remote.process()).thenThrow(new IgnoredException("ignore IgnoredException"));
    for (int i = 0; i < 5; i++) {
      UserResponse response = business.businessProcess();
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
      UserResponse response = business.businessProcess();
      assertEquals(State.CLOSED, response.getState());
    }
    // open circuit breaker when hits 5 requests
    for (int i = 0; i < 2; i++) {
      UserResponse response = business.businessProcess();
      assertEquals(State.OPEN, response.getState());
    }
    // 6th time would not hits "remoteService.process()" because circuit breaker is opened
    Mockito.verify(remote, times(5)).process();
  }
}
