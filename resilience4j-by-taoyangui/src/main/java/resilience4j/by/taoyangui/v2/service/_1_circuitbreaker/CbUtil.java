package resilience4j.by.taoyangui.v2.service._1_circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CbUtil {
  public static final String SCENARIO_1 = "scenario1";
  public static final String SCENARIO_2 = "scenario2";

  public static void getStatus(String message, CircuitBreaker circuitBreaker) {
    CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
    // 当环满了就计算失败率. 例如, 如果Ring Bit Buffer的大小设置为10, 如果前9次的请求调用都失败也不会计算请求调用失败率
    float failureRate = metrics.getFailureRate(); // failure rate in percentage
    int bufferedCalls = metrics.getNumberOfBufferedCalls(); // current number of buffered calls
    int failedCalls = metrics.getNumberOfFailedCalls(); // current number of failed calls
    int successCalls = metrics.getNumberOfSuccessfulCalls(); // current number of succeed
    long notPermittedCalls =
        metrics.getNumberOfNotPermittedCalls(); // current number of not permitted calls

    log.info(
        message
            + "state="
            + circuitBreaker.getState()
            + ", metrics [ failureRate="
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

  public static CircuitBreaker addCircuitBreakerListener(CircuitBreaker circuitBreaker) {
    circuitBreaker
        .getEventPublisher()
        .onSuccess(event -> log.info("  |- 服务调用成功: " + event.toString()))
        .onError(event -> log.info("  |- 服务调用失败: " + event.toString()))
        .onIgnoredError(event -> log.info("  |- 服务调用失败，但异常被忽略: " + event.toString()))
        .onReset(event -> log.info("  |- 熔断器重置: " + event.toString()))
        .onStateTransition(event -> log.info("  |- 熔断器状态改变: " + event.toString()))
        .onCallNotPermitted(event -> log.info("  |- 熔断器已经打开: " + event.toString()));
    return circuitBreaker;
  }
}
