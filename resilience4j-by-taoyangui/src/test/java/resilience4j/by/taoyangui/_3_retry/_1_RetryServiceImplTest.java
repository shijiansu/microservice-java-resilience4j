package resilience4j.by.taoyangui._3_retry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.SCENARIO_1;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
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
public class _1_RetryServiceImplTest {
  @Autowired
  @Qualifier("retryServiceImpl")
  BusinessService service;

  @MockBean RemoteService remote;
  @Autowired UserRepository repository;

  @Autowired CircuitBreakerRegistry cbRegistry;
  CircuitBreaker cb;

  @Autowired RetryRegistry rtRegistry;
  Retry rt;

  @BeforeEach
  public void init() {
    rt = rtRegistry.retry(SCENARIO_1);
    cb = cbRegistry.circuitBreaker(SCENARIO_1);
  }

  @Order(1)
  @Test
  public void retry() {
    IgnoredException a = new IgnoredException("ignore IgnoredException");
    ProblemException b = new ProblemException("count ProblemException");

    Mockito.when(remote.process())
        .thenThrow(a) // 1st
        .thenReturn(repository.findAll()) // 2nd
        .thenThrow(b) // 3rd
        .thenReturn(repository.findAll()) // 3rd - retry
        .thenThrow(b, b, a) // 4th
        .thenThrow(b) // 5th, 6th, and retry also hit BusinessBException
    ;
    for (int i = 0; i < 6; i++) {
      // ignore 2 times, succ 2 times, failed 2 times
      System.out.println("Request #: " + i);
      UserResponse response = service.businessProcess();
      assertEquals(State.CLOSED, response.getState());
    }
    // failed 3rd time, hit 5 counted requests, OPEN the CircuitBreaker
    UserResponse response = service.businessProcess();
    assertEquals(State.OPEN, response.getState());
    Mockito.verify(
            remote,
            times(
                1 // ignored exception at retry
                    + 1 // success
                    + (1 + 1) // after 1 retry then success
                    + (1 + 2) // after 2 retry then ignored exception
                    + (1 + 3) // all fails, 3 times retry
                    + (1 + 3) // all fails, 3 times retry
                    + (1 + 3) // all fails, 3 times retry
                ))
        .process();
  }
}
