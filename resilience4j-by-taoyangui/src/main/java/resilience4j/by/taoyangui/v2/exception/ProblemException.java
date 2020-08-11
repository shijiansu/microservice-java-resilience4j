package resilience4j.by.taoyangui.v2.exception;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ProblemException extends RuntimeException {

  // original name is "BusinessBException"

  public ProblemException(String message) {
    super(message);
  }
}
