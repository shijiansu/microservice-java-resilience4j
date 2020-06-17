package microservice.java.resilience4j.ratelimiter;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BackendService {
  private static final SimpleDateFormat formatter =
      new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss:SSS");

  public static String now() {
    return formatter.format(new Date());
  }

  public static void log(String where, Object message) {
    System.out.println(
        "["
            + now()
            + "] - ["
            + Thread.currentThread().getName()
            + "] - "
            + where
            + " - "
            + message);
  }

  public static void sleep(int delay) {
    try {
      Thread.sleep(delay);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public String doSomething() {
    return "Hello world";
  }
}
