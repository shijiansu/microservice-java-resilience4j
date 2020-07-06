package resilience4j.by.taoyangui.remote_service;

import java.util.List;
import resilience4j.by.taoyangui.data.User;

public interface RemoteService {
  List<User> process();
}
