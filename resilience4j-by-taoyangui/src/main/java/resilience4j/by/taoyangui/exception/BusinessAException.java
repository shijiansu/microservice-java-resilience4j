package resilience4j.by.taoyangui.exception;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class BusinessAException extends RuntimeException {

  public BusinessAException(String message) {
    super(message);
    log.error(message);
  }
}
