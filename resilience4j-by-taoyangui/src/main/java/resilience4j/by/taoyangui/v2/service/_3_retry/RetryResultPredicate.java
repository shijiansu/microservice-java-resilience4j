package resilience4j.by.taoyangui.v2.service._3_retry;

import java.util.function.Predicate;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class RetryResultPredicate implements Predicate {

  @Override
  public boolean test(Object o) {
    return o == null;
  }
}
