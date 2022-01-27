package org.broadinstitute.listener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@SpringBootConfiguration
@EnableAutoConfiguration
public class ListenerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ListenerApplication.class, args);
  }
}
