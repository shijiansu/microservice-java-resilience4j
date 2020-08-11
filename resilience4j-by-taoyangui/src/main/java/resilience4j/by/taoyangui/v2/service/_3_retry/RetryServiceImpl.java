package resilience4j.by.taoyangui.v2.service._3_retry;

import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.SCENARIO_1;
import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.getStatus;
import static resilience4j.by.taoyangui.v2.service._3_retry.RetryUtil.addRetryListener;
import static resilience4j.by.taoyangui.v2.service._3_retry.RetryUtil.getRetryStatus;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.CheckedFunction0;
import io.vavr.control.Try;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import resilience4j.by.taoyangui.v2.data.User;
import resilience4j.by.taoyangui.v2.data.UserResponse;
import resilience4j.by.taoyangui.v2.service.business.BusinessService;
import resilience4j.by.taoyangui.v2.service.integration.IntegrationConnector;

@Log4j2
@Service("retryServiceImpl")
public class RetryServiceImpl implements BusinessService {
  @Autowired
  @Qualifier("cbConnectorImpl")
  IntegrationConnector connector;

  @Autowired RetryRegistry rtRegistry;
  Retry rt;

  @Autowired CircuitBreakerRegistry cbRegistry;
  CircuitBreaker cb;
  CheckedFunction0<List<User>> cFunction;

  @PostConstruct
  public void init() {
    rt = rtRegistry.retry(SCENARIO_1);
    addRetryListener(rt);
    CheckedFunction0<List<User>> checkedSupplier =
        Retry.decorateCheckedSupplier(rt, connector::process);

    cb = cbRegistry.circuitBreaker(SCENARIO_1);
    // addCircuitBreakerListener(cb); // 这个listener不单例, 如果在其他地方也注册了这个listener, 则listener多次
    cFunction = CircuitBreaker.decorateCheckedSupplier(cb, checkedSupplier);
  }

  public UserResponse businessProcess() {
    getStatus("执行开始前: ", cb);
    getRetryStatus("执行开始前 - 重试: ", rt);
    // use Try.of().recover() to deal with fallback
    Try<List<User>> result =
        Try.of(cFunction)
            .recover(
                CallNotPermittedException.class,
                t -> {
                  log.error("熔断器已经打开, 拒绝访问被保护方法: " + t.getLocalizedMessage());
                  getStatus("熔断器打开中:", cb);
                  return Collections.emptyList();
                })
            .recover(
                t -> {
                  log.error("方法被降级了: " + t.getLocalizedMessage());
                  getStatus("降级方法中:", cb);
                  return Collections.emptyList();
                });
    getRetryStatus("执行结束后 - 重试: ", rt);
    getStatus("执行结束后: ", cb);
    System.out.println();
    return new UserResponse(result.get(), cb.getState());
  }
}
