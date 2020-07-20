package resilience4j.by.taoyangui.service.retry;

import static resilience4j.by.taoyangui.service.circuitbreaker.CircuitBreakerUtil.BACKEND_A;
import static resilience4j.by.taoyangui.service.circuitbreaker.CircuitBreakerUtil.getStatus;
import static resilience4j.by.taoyangui.service.retry.RetryUtil.addRetryListener;
import static resilience4j.by.taoyangui.service.retry.RetryUtil.getRetryStatus;

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
import org.springframework.stereotype.Service;
import resilience4j.by.taoyangui.data.Response;
import resilience4j.by.taoyangui.data.User;
import resilience4j.by.taoyangui.remote.RemoteServiceConnector;
import resilience4j.by.taoyangui.service.BusinessService;

@Log4j2
@Service("retryBusinessServiceNonAopImpl")
public class RetryBusinessServiceNonAopImpl implements BusinessService {
  @Autowired CircuitBreakerRegistry cbRegistry;

  @Autowired RemoteServiceConnector connector;

  CircuitBreaker cb;

  @Autowired RetryRegistry rtRegistry;

  Retry rt;

  CheckedFunction0<List<User>> chainedSupplier;

  @PostConstruct
  void init() {
    cb = cbRegistry.circuitBreaker(BACKEND_A);
    rt = rtRegistry.retry(BACKEND_A);

    addRetryListener(rt);

    CheckedFunction0<List<User>> checkedSupplier =
        Retry.decorateCheckedSupplier(rt, connector::process);
    chainedSupplier = CircuitBreaker.decorateCheckedSupplier(cb, checkedSupplier);
  }

  public Response businessProcess() {
    getStatus("执行开始前: ", cb);
    getRetryStatus("执行开始前 - 重试: ", rt);
    // use Try.of().recover() to deal with fallback
    Try<List<User>> result =
        Try.of(chainedSupplier)
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
    return new Response(result.get(), cb.getState());
  }
}
