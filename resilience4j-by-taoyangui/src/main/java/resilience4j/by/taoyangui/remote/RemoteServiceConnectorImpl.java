package resilience4j.by.taoyangui.remote;

import static resilience4j.by.taoyangui.service.circuitbreaker.CircuitBreakerUtil.BACKEND_A;
import static resilience4j.by.taoyangui.service.circuitbreaker.CircuitBreakerUtil.BACKEND_B;
import static resilience4j.by.taoyangui.service.circuitbreaker.CircuitBreakerUtil.getStatus;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collections;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import resilience4j.by.taoyangui.data.User;
import resilience4j.by.taoyangui.remote_service.RemoteService;

@Log4j2
@Service
public class RemoteServiceConnectorImpl implements RemoteServiceConnector {

  @Autowired CircuitBreakerRegistry registry;

  @Autowired RemoteService remoteService;

  @Override
  public List<User> process() {
    return remoteService.process();
  }

  // 通过Spring AOP机制, 所以需要导入starter-aop jar
  @Override
  @CircuitBreaker(name = BACKEND_A, fallbackMethod = "fallback")
  public List<User> process2() {
    return remoteService.process();
  }

  // 通过Spring AOP机制, 所以需要导入starter-aop jar
  @Override
  @CircuitBreaker(name = BACKEND_A, fallbackMethod = "fallback")
  @Retry(name = BACKEND_A, fallbackMethod = "fallBack")
  public List<User> process3() {
    return remoteService.process();
  }


  // It's important to remember that a fallback method should be placed in the same class and must
  // have the same method signature with just ONE extra target exception parameter.
  private List<User> fallback(Throwable t) {
    log.error("方法被降级了: " + t.getLocalizedMessage());
    getStatus("降级方法中: ", registry.circuitBreaker(BACKEND_B));
    return Collections.emptyList();
  }

  private List<User> fallback(CallNotPermittedException t) {
    log.error("熔断器已经打开, 拒绝访问被保护方法: " + t.getLocalizedMessage());
    getStatus("熔断器打开中: ", registry.circuitBreaker(BACKEND_B));
    return Collections.emptyList();
  }
}
