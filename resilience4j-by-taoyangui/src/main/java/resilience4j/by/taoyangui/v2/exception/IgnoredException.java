package resilience4j.by.taoyangui.v2.exception;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class IgnoredException extends RuntimeException {

  // original name is "BusinessAException"

  public IgnoredException(String message) {
    super(message);
  }
}
