package resilience4j.by.taoyangui.v2.service._1_circuitbreaker;

import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.SCENARIO_1;
import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.getStatus;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
@Service("cbServiceAopImpl")
public class CbServiceAopImpl implements BusinessService {
  @Autowired
  @Qualifier("cbConnectorAopImpl")
  IntegrationConnector connector;

  @Autowired CircuitBreakerRegistry cbRegistry;
  CircuitBreaker cb;

  @PostConstruct
  public void init() {
    cb = cbRegistry.circuitBreaker(SCENARIO_1);
  }

  public UserResponse businessProcess() {
    getStatus("执行开始前: ", cb);
    List<User> result = connector.process();
    getStatus("执行结束后: ", cb);
    System.out.println();
    return new UserResponse(result, cb.getState());
  }
}
