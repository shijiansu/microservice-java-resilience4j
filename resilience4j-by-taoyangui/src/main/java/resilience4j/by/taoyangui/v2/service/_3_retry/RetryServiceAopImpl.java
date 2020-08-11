package resilience4j.by.taoyangui.v2.service._3_retry;

import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.SCENARIO_1;
import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.getStatus;
import static resilience4j.by.taoyangui.v2.service._3_retry.RetryUtil.getRetryStatus;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
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
@Service("retryServiceAopImpl")
public class RetryServiceAopImpl implements BusinessService {
  @Autowired
  @Qualifier("cbConnectorAopImpl")
  IntegrationConnector connector;

  @Autowired RetryRegistry rtRegistry;
  Retry rt;

  @Autowired CircuitBreakerRegistry cbRegistry;
  CircuitBreaker cb;

  @PostConstruct
  public void init() {
    cb = cbRegistry.circuitBreaker(SCENARIO_1);
    rt = rtRegistry.retry(SCENARIO_1);
  }

  public UserResponse businessProcess() {
    getStatus("执行开始前: ", cb);
    getRetryStatus("执行开始前 - 重试: ", rt);
    List<User> result = connector.process();
    getRetryStatus("执行结束 - 重试: ", rt);
    getStatus("执行结束后: ", cb);
    System.out.println();
    return new UserResponse(result, cb.getState());
  }
}
