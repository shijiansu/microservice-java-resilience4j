package resilience4j.by.taoyangui.service.retry;

import java.util.function.Predicate;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class RetryExceptionPredicate implements Predicate {

  @Override
  public boolean test(Object o) {
    return true;
  }
}
