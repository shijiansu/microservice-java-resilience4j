package resilience4j.by.taoyangui.remote;

import static resilience4j.by.taoyangui.CircuitBreakerUtil.BACKEND_B;
import static resilience4j.by.taoyangui.CircuitBreakerUtil.getStatus;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Collections;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import resilience4j.by.taoyangui.data.User;
import resilience4j.by.taoyangui.remote_service.RemoteService;

@Log4j2
@Service
public class RemoteServiceConnector {

  @Autowired CircuitBreakerRegistry registry;

  @Autowired RemoteService remoteService;

  public List<User> process() {
    return remoteService.process();
  }

  @CircuitBreaker(name = BACKEND_B, fallbackMethod = "fallback")
  public List<User> process2() {
    return remoteService.process();
  }

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
