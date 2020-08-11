package resilience4j.by.taoyangui.v2.service._4_bulkhead;

import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import resilience4j.by.taoyangui.v2.data.User;
import resilience4j.by.taoyangui.v2.service.integration.IntegrationConnector;
import resilience4j.by.taoyangui.v2.service.remote.RemoteService;

@Log4j2
@Service("bhConnectorImpl")
public class BhConnectorImpl implements IntegrationConnector {

  @Autowired RemoteService remote;

  @Override
  public List<User> process() {
    return remote.process();
  }
}
