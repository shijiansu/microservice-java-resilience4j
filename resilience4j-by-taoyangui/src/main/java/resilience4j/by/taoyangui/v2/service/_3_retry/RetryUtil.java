package resilience4j.by.taoyangui.v2.service._3_retry;

import io.github.resilience4j.retry.Retry;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class RetryUtil {
  public static void getRetryStatus(String message, Retry retry) {
    Retry.Metrics metrics = retry.getMetrics();
    long failedCallWithRetry = metrics.getNumberOfFailedCallsWithRetryAttempt();
    long failedCallWithoutRetry = metrics.getNumberOfFailedCallsWithoutRetryAttempt();
    long succCallWithRetry = metrics.getNumberOfSuccessfulCallsWithRetryAttempt();
    long succCallWithoutRetry = metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt();

    log.info(
        message
            + "state="
            + " metrics [ failedCallWithRetry="
            + failedCallWithRetry
            + ", failedCallWithoutRetry="
            + failedCallWithoutRetry
            + ", succCallWithRetry="
            + succCallWithRetry
            + ", succCallWithoutRetry="
            + succCallWithoutRetry
            + " ]");
  }

  public static Retry addRetryListener(Retry retry) {
    retry
        .getEventPublisher()
        .onSuccess(event -> log.info("  |- 服务调用成功 (一旦遇到成功): " + event.toString()))
        .onError(event -> log.info("  |- 服务调用失败 (在所有重试失败后): " + event.toString()))
        .onIgnoredError(event -> log.info("  |- 服务调用失败，但异常被忽略 (在所有重试失败后): " + event.toString()))
        .onRetry(event -> log.info("  |- 重试: 第" + event.getNumberOfRetryAttempts() + "次"));
    return retry;
  }
}
