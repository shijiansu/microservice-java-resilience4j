package resilience4j.by.taoyangui.v2.data;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserRepository {

  public List<User> findAll() {
    return Arrays.asList(
        new User("Tester 1", 30), new User("Tester 2", 40), new User("Tester 3", 50));
  }
}
