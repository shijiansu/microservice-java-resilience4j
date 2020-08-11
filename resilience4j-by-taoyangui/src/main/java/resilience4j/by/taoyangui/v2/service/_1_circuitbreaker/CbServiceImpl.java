package resilience4j.by.taoyangui.v2.service._1_circuitbreaker;

import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.SCENARIO_1;
import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.addCircuitBreakerListener;
import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.getStatus;

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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import resilience4j.by.taoyangui.v2.data.User;
import resilience4j.by.taoyangui.v2.data.UserResponse;
import resilience4j.by.taoyangui.v2.service.business.BusinessService;
import resilience4j.by.taoyangui.v2.service.integration.IntegrationConnector;

@Log4j2
@Service("cbServiceImpl")
public class CbServiceImpl implements BusinessService {
  @Autowired
  @Qualifier("cbConnectorImpl")
  IntegrationConnector connector;

  // auto load properties from Spring configuration
  @Autowired CircuitBreakerRegistry cbRegistry;
  CircuitBreaker cb;
  CheckedFunction0<List<User>> cFunction;

  @PostConstruct
  public void init() {
    cb = cbRegistry.circuitBreaker(SCENARIO_1);
    addCircuitBreakerListener(cb);
    cFunction = CircuitBreaker.decorateCheckedSupplier(cb, connector::process);
  }

  public UserResponse businessProcess() {
    getStatus("执行开始前: ", cb);
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
                  getStatus("降级方法中: ", cb);
                  return Collections.emptyList();
                });
    getStatus("执行结束后: ", cb);
    System.out.println();
    return new UserResponse(result.get(), cb.getState());
  }
}
