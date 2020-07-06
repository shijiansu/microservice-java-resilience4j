package resilience4j.by.taoyangui;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CircuitBreakerUtil {
  public static final String BACKEND_A = "backendA";
  public static final String BACKEND_B = "backendB";

  public static void getStatus(String time, CircuitBreaker circuitBreaker) {
    CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
    // 当环满了就计算失败率. 例如, 如果Ring Bit Buffer的大小设置为10, 如果前9次的请求调用都失败也不会计算请求调用失败率
    float failureRate = metrics.getFailureRate(); // failure rate in percentage
    int bufferedCalls = metrics.getNumberOfBufferedCalls(); // current number of buffered calls
    int failedCalls = metrics.getNumberOfFailedCalls(); // current number of failed calls
    int successCalls = metrics.getNumberOfSuccessfulCalls(); // current number of successed
    long notPermittedCalls =
        metrics.getNumberOfNotPermittedCalls(); // current number of not permitted calls

    log.info(
        time
            + "state="
            + circuitBreaker.getState()
            + ", metrics[ failureRate="
            + failureRate
            + ", bufferedCalls="
            + bufferedCalls
            + ", failedCalls="
            + failedCalls
            + ", successCalls="
            + successCalls
            + ", notPermittedCalls="
            + notPermittedCalls
            + " ]");
  }
}
