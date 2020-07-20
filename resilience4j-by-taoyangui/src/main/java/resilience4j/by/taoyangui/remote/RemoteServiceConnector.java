package resilience4j.by.taoyangui.remote;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import resilience4j.by.taoyangui.data.User;

public interface RemoteServiceConnector {

  List<User> process();

  List<User> process2();

  List<User> process3();

}
