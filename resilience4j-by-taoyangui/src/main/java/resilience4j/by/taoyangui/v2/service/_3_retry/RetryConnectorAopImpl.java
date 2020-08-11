package resilience4j.by.taoyangui.v2.service._3_retry;

import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.SCENARIO_1;
import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.getStatus;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collections;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import resilience4j.by.taoyangui.v2.data.User;
import resilience4j.by.taoyangui.v2.service.integration.IntegrationConnector;
import resilience4j.by.taoyangui.v2.service.remote.RemoteService;

@Log4j2
@Service("retryConnectorAopImpl")
public class RetryConnectorAopImpl implements IntegrationConnector {

  @Autowired CircuitBreakerRegistry registry;

  @Autowired RemoteService remote;

  // 通过Spring AOP机制, 所以需要导入starter-aop jar
  @Override
  @Retry(name = SCENARIO_1, fallbackMethod = "fallBack")
  @CircuitBreaker(name = SCENARIO_1, fallbackMethod = "fallback")
  // 如果Retry, CircuitBreaker, Bulkhead同时注解在方法上,
  // 默认的顺序是Retry > CircuitBreaker > Bulkhead, 即先控制并发再熔断最后重试
  public List<User> process() {
    return remote.process();
  }

  // It's important to remember that a fallback method should be placed in the same class and must
  // have the same method signature with just ONE extra target exception parameter.
  private List<User> fallback(Throwable t) {
    log.error("方法被降级了: " + t.getLocalizedMessage());
    getStatus("降级方法中: ", registry.circuitBreaker(SCENARIO_1));
    return Collections.emptyList();
  }

  private List<User> fallback(CallNotPermittedException t) {
    log.error("熔断器已经打开, 拒绝访问被保护方法: " + t.getLocalizedMessage());
    getStatus("熔断器打开中: ", registry.circuitBreaker(SCENARIO_1));
    return Collections.emptyList();
  }
}
