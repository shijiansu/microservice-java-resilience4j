package resilience4j.by.taoyangui.service;

import static resilience4j.by.taoyangui.CircuitBreakerUtil.BACKEND_B;
import static resilience4j.by.taoyangui.CircuitBreakerUtil.getStatus;

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

@Log4j2
@Service
public class CircuitBreakerServiceAopImpl implements CircuitBreakerService {
  @Autowired CircuitBreakerRegistry registry;

  @Autowired RemoteServiceConnector connector;

  CircuitBreaker cb;

  @PostConstruct
  void init() {
    cb = registry.circuitBreaker(BACKEND_B);
  }

  public Response circuitBreak() {
    getStatus("执行开始前: ", cb);
    List<User> result = connector.process2();
    getStatus("执行结束后: ", cb);
    return new Response(result, cb.getState());
  }
}
