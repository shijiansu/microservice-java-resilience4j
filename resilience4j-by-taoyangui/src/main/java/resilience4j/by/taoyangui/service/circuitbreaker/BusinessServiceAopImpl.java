package resilience4j.by.taoyangui.service.circuitbreaker;

import static resilience4j.by.taoyangui.service.circuitbreaker.CircuitBreakerUtil.BACKEND_A;
import static resilience4j.by.taoyangui.service.circuitbreaker.CircuitBreakerUtil.getStatus;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
@Service("businessServiceAopImpl")
public class BusinessServiceAopImpl implements BusinessService {
  @Autowired CircuitBreakerRegistry cbRegistry;

  @Autowired
  RemoteServiceConnector connector;

  CircuitBreaker cb;

  @PostConstruct
  void init() {
    cb = cbRegistry.circuitBreaker(BACKEND_A);
  }

  public Response businessProcess() {
    getStatus("执行开始前: ", cb);
    List<User> result = connector.process2();
    getStatus("执行结束后: ", cb);
    System.out.println();
    return new Response(result, cb.getState());
  }
}
