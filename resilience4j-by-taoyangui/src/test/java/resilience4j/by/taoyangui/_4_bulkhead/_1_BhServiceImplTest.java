package resilience4j.by.taoyangui._4_bulkhead;

import static resilience4j.by.taoyangui.v2.service._1_circuitbreaker.CbUtil.SCENARIO_1;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import resilience4j.by.taoyangui.v2.data.User;
import resilience4j.by.taoyangui.v2.data.UserRepository;
import resilience4j.by.taoyangui.v2.service.business.BusinessService;
import resilience4j.by.taoyangui.v2.service.remote.RemoteService;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class _1_BhServiceImplTest {
  @Autowired
  @Qualifier("bhServiceImpl")
  BusinessService service;

  @MockBean RemoteService remote;
  @Autowired UserRepository repository;

  @Autowired BulkheadRegistry bhRegistry;
  Bulkhead bh;

  @BeforeEach
  public void init() {
    bh = bhRegistry.bulkhead(SCENARIO_1);
  }

  @SneakyThrows
  @Order(1)
  @Test
  public void bulkhead() {
    int total = 5;
    for (int i = 0; i < total; i++) {
      Mockito.when(remote.process()).then((Answer<List<User>>) invocation -> repository.findAll());
    }
    CountDownLatch latch = new CountDownLatch(total);
    for (int i = 0; i < total; i++) {
      new Thread(
              () -> {
                service.businessProcess();
                latch.countDown();
              })
          .start();
    }

    //      IntStream.range(0, total)
    //        .forEachOrdered(
    //            i ->
    //                new Thread(
    //                        () -> {
    //                          System.out.println(i);
    //                          service.businessProcess();
    //                          latch.countDown();
    //                        })
    //                    .start());
    latch.await(total, TimeUnit.MINUTES);
  }
}
