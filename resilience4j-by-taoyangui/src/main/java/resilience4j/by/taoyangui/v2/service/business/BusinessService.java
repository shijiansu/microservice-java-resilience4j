package resilience4j.by.taoyangui.v2.service.business;

import resilience4j.by.taoyangui.v2.data.UserResponse;

public interface BusinessService {

  void init();

  UserResponse businessProcess();
}
