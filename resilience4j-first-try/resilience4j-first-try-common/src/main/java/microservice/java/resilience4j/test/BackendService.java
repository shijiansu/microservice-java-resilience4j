package microservice.java.resilience4j.test;

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

  public String doSomethingSlowly() {
    sleep(3000);
    log("BACKEND SERVICE", "doSomethingSlowly");
    return "Hello world";
  }

  public String doSomethingThrowException() {
    log("BACKEND SERVICE", "doSomethingThrowException");
    throw new RuntimeException("Hello exception");
  }

  public String doSomethingWithArgs(String message) {
    log("BACKEND SERVICE", "doSomethingWithArgs - " + message);
    return "Hello " + message;
  }

}
