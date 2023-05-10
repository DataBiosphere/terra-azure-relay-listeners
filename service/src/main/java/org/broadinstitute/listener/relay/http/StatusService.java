package org.broadinstitute.listener.relay.http;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusService {

  @GetMapping("/status")
  public ResponseEntity<String> getStatus() {
    return ResponseEntity.ok("Application is running.");
  }
}
