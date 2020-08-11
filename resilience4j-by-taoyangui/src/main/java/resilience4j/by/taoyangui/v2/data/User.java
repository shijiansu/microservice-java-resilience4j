package resilience4j.by.taoyangui.v2.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class User {
  private String name;
  private Integer age;
}
