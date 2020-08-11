package resilience4j.by.taoyangui.v2.data;

import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {
  List<User> users;
  State state;
}
