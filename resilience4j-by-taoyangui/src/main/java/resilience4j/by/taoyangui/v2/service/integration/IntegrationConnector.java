package resilience4j.by.taoyangui.v2.service.integration;

import java.util.List;
import resilience4j.by.taoyangui.v2.data.User;

public interface IntegrationConnector {

  List<User> process();
}
