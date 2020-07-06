package resilience4j.by.taoyangui.service;

import resilience4j.by.taoyangui.data.Response;

public interface CircuitBreakerService {

  Response circuitBreak();
}
