package resilience4j.by.taoyangui.service.retry;

import java.util.function.Predicate;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class RetryResultPredicate implements Predicate {

  @Override
  public boolean test(Object o) {
    System.out.println("asdfasdfasdfasdfasd");
    return o == null;
  }
}
