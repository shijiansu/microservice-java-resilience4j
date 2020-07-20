package resilience4j.by.taoyangui._2_timelimiter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static resilience4j.by.taoyangui.service.circuitbreaker.CircuitBreakerUtil.BACKEND_A;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import resilience4j.by.taoyangui.data.Response;
import resilience4j.by.taoyangui.data.User;
import resilience4j.by.taoyangui.data.UserRepository;
import resilience4j.by.taoyangui.remote_service.RemoteService;
import resilience4j.by.taoyangui.service.BusinessService;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class TimeLimiterBusinessServiceNonAopImplTest {
  @Autowired
  @Qualifier("timeLimiterBusinessServiceNonAopImpl")
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
  void timeout() {
    Mockito.when(remoteService.process())
        .then(
            (Answer<List<User>>)
                invocation -> {
                  Thread.sleep(3000);
                  return repository.findAll();
                });
    for (int i = 0; i < 5; i++) {
      Response response = businessService.businessProcess();
      assertEquals(i != 4 ? State.CLOSED : State.OPEN, response.getState());
    }
    Mockito.verify(remoteService, times(5)).process();
  }
}
