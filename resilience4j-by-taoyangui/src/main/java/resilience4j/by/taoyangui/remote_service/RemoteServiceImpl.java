package resilience4j.by.taoyangui.remote_service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import resilience4j.by.taoyangui.data.User;
import resilience4j.by.taoyangui.data.UserRepository;
import resilience4j.by.taoyangui.exception.BusinessAException;
import resilience4j.by.taoyangui.exception.BusinessBException;

@Log4j2
@Service
public class RemoteServiceImpl implements RemoteService {
  @Autowired UserRepository repository;

  public static final AtomicInteger COUNT = new AtomicInteger(0);

  public List<User> process() {
    int num = COUNT.getAndIncrement();
    log.info("count的值 = " + num);
    if (num % 4 == 1) {
      throw new BusinessAException("异常A");
    } else if (num % 4 == 2 || num % 4 == 3) {
      throw new BusinessBException("异常B");
    }
    log.info("服务正常运行, 获取用户列表");
    return repository.findAll();
  }
}
