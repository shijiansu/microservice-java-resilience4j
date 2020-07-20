package resilience4j.by.taoyangui.service.circuitbreaker;

import static resilience4j.by.taoyangui.service.circuitbreaker.CircuitBreakerUtil.BACKEND_A;
import static resilience4j.by.taoyangui.service.circuitbreaker.CircuitBreakerUtil.getStatus;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
@Service("businessServiceNonAopImpl")
public class BusinessServiceNonAopImpl implements BusinessService {
  @Autowired CircuitBreakerRegistry cbRegistry;

  @Autowired RemoteServiceConnector connector;

  CircuitBreaker cb;

  CheckedFunction0<List<User>> checkedSupplier;

  @PostConstruct
  void init() {
    cb = cbRegistry.circuitBreaker(BACKEND_A);
    // addCircuitBreakerListener(cb);
    checkedSupplier = CircuitBreaker.decorateCheckedSupplier(cb, connector::process);
  }

  public Response businessProcess() {
    getStatus("执行开始前: ", cb);
    // use Try.of().recover() to deal with fallback
    Try<List<User>> result =
        Try.of(checkedSupplier)
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
    getStatus("执行结束后: ", cb);
    System.out.println();
    return new Response(result.get(), cb.getState());
  }
}