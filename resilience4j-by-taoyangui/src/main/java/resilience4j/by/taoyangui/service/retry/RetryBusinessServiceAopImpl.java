package resilience4j.by.taoyangui.service.retry;

import static resilience4j.by.taoyangui.service.circuitbreaker.CircuitBreakerUtil.BACKEND_A;
import static resilience4j.by.taoyangui.service.circuitbreaker.CircuitBreakerUtil.getStatus;
import static resilience4j.by.taoyangui.service.retry.RetryUtil.getRetryStatus;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
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
@Service("retryBusinessServiceAopImpl")
public class RetryBusinessServiceAopImpl implements BusinessService {
  @Autowired CircuitBreakerRegistry cbRegistry;

  @Autowired RemoteServiceConnector connector;

  CircuitBreaker cb;

  @Autowired RetryRegistry rtRegistry;

  Retry rt;

  @PostConstruct
  void init() {
    cb = cbRegistry.circuitBreaker(BACKEND_A);
    rt = rtRegistry.retry(BACKEND_A);
  }

  public Response businessProcess() {
    getStatus("执行开始前: ", cb);
    getRetryStatus("执行开始前 - 重试: ", rt);
    List<User> result = connector.process3();
    getRetryStatus("执行结束 - 重试: ", rt);
    getStatus("执行结束后: ", cb);
    System.out.println();
    return new Response(result, cb.getState());
  }
}
