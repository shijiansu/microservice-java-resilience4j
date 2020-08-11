package resilience4j.by.taoyangui.v2.service._4_bulkhead;

import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.SCENARIO_1;
import static resilience4j.by.taoyangui.v2.service._4_bulkhead.BulkhdadUtil.addBulkheadListener;
import static resilience4j.by.taoyangui.v2.service._4_bulkhead.BulkhdadUtil.getBulkheadStatus;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.vavr.CheckedFunction0;
import io.vavr.control.Try;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import resilience4j.by.taoyangui.v2.data.User;
import resilience4j.by.taoyangui.v2.data.UserResponse;
import resilience4j.by.taoyangui.v2.service.business.BusinessService;
import resilience4j.by.taoyangui.v2.service.integration.IntegrationConnector;

@Log4j2
@Service("bhServiceImpl")
public class BhServiceImpl implements BusinessService {
  @Autowired
  @Qualifier("bhConnectorImpl")
  IntegrationConnector connector;

  @Autowired BulkheadRegistry bhRegistry;
  Bulkhead bh;
  CheckedFunction0<List<User>> cFunction;

  @PostConstruct
  @Override
  public void init() {
    bh = bhRegistry.bulkhead(SCENARIO_1);
    addBulkheadListener(bh);
    cFunction = Bulkhead.decorateCheckedSupplier(bh, connector::process);
  }

  @Override
  public UserResponse businessProcess() {
    getBulkheadStatus("开始执行前: ", bh);
    // 通过Try.of().recover()调用装饰后的服务
    Try<List<User>> result =
        Try.of(cFunction)
            .recover(
                BulkheadFullException.class,
                throwable -> {
                  log.info("服务失败: " + throwable.getLocalizedMessage());
                  return Collections.emptyList();
                });
    getBulkheadStatus("执行结束后: ", bh);
    System.out.println();
    return new UserResponse(result.get(), null);
  }
}
