package resilience4j.by.taoyangui.v2.service.remote;

import java.util.List;
import resilience4j.by.taoyangui.v2.data.User;

public interface RemoteService {
  List<User> process();
}
