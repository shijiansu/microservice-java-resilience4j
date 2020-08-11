package resilience4j.by.taoyangui.v2.service._4_bulkhead;

import io.github.resilience4j.bulkhead.Bulkhead;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class BulkhdadUtil {
  public static void getBulkheadStatus(String message, Bulkhead bulkhead) {
    Bulkhead.Metrics metrics = bulkhead.getMetrics();
    // Returns the number of parallel executions this bulkhead can support at this point in time.
    int availableConcurrentCalls = metrics.getAvailableConcurrentCalls();
    // Returns the configured max amount of concurrent calls
    int maxAllowedConcurrentCalls = metrics.getMaxAllowedConcurrentCalls();

    log.info(
        message
            + ", metrics [ availableConcurrentCalls="
            + availableConcurrentCalls
            + ", maxAllowedConcurrentCalls="
            + maxAllowedConcurrentCalls
            + " ]");
  }

  public static Bulkhead addBulkheadListener(Bulkhead bulkhead) {
    bulkhead
        .getEventPublisher()
        .onCallFinished(event -> log.info("  |- Call完成: " + event.toString()))
        .onCallPermitted(event -> log.info("  |- Call允许进入: " + event.toString()))
        .onCallRejected(event -> log.info("  |- Call禁止进入: " + event.toString()));
    return bulkhead;
  }
}
