package resilience4j.by.taoyangui.v2.service._2_timelimiter;

import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.SCENARIO_1;
import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.getStatus;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.vavr.control.Try;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
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
@Service("tlServiceImpl")
public class TlServiceImpl implements BusinessService {
  @Autowired
  @Qualifier("cbConnectorImpl")
  IntegrationConnector connector;

  @Autowired CircuitBreakerRegistry cbRegistry;
  @Autowired TimeLimiterRegistry tlRegistry;
  CircuitBreaker cb;
  Callable<List<User>> chainedCallable;

  @PostConstruct
  public void init() {
    cb = cbRegistry.circuitBreaker(SCENARIO_1);
    // 因为TimeLimter是基于Future的get方法的，所以需要创建线程池，然后通过线程池的submit方法获取Future对象
    TimeLimiter tl = tlRegistry.timeLimiter(SCENARIO_1);
    // 创建单线程的线程池
    ExecutorService pool = Executors.newSingleThreadExecutor();
    // 将被保护方法包装为能够返回Future的supplier函数
    Supplier<Future<List<User>>> futureSupplier = () -> pool.submit(connector::process);
    // 先用限时器包装，再用熔断器包装
    Callable<List<User>> restrictedCall = TimeLimiter.decorateFutureSupplier(tl, futureSupplier);
    chainedCallable = CircuitBreaker.decorateCallable(cb, restrictedCall);
  }

  public UserResponse businessProcess() {
    getStatus("执行开始前: ", cb);
    // use Try.of().recover() to deal with fallback
    Try<List<User>> result =
        Try.of(chainedCallable::call)
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
