package resilience4j.by.taoyangui._3_retry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static resilience4j.by.taoyangui.service.circuitbreaker.CircuitBreakerUtil.BACKEND_A;

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
import resilience4j.by.taoyangui.data.Response;
import resilience4j.by.taoyangui.data.UserRepository;
import resilience4j.by.taoyangui.exception.BusinessAException;
import resilience4j.by.taoyangui.exception.BusinessBException;
import resilience4j.by.taoyangui.remote_service.RemoteService;
import resilience4j.by.taoyangui.service.BusinessService;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class RetryBusinessServiceNonAopImplTest {
  @Autowired
  @Qualifier("retryBusinessServiceNonAopImpl")
  BusinessService businessService;

  @MockBean RemoteService remoteService;
  @Autowired UserRepository repository;

  @Autowired CircuitBreakerRegistry cbRegistry;
  CircuitBreaker cb;

  @Autowired RetryRegistry rtRegistry;
  Retry rt;

  @BeforeEach
  public void init() {
    cb = cbRegistry.circuitBreaker(BACKEND_A);
    rt = rtRegistry.retry(BACKEND_A);
  }

  @Order(1)
  @Test
  void retry() {
    BusinessAException a = new BusinessAException("ignore A exception");
    BusinessBException b = new BusinessBException("count B exception");

    Mockito.when(remoteService.process())
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
      Response response = businessService.businessProcess();
      assertEquals(State.CLOSED, response.getState());
    }
    // failed 3rd time, hit 5 counted requests, OPEN the CircuitBreaker
    Response response = businessService.businessProcess();
    assertEquals(State.OPEN, response.getState());
    Mockito.verify(
            remoteService,
            times(
                1 // ignore at retry
                    + 1 // success
                    + (1 + 1) // after 1 retry then success
                    + (1 + 2) // after 2 retry then ignore
                    + (1 + 3) // all fails, 3 times retry
                    + (1 + 3) // all fails, 3 times retry
                    + (1 + 3) // all fails, 3 times retry
                ))
        .process();
  }
}
