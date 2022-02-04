package org.broadinstitute.listener.relay.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import com.microsoft.azure.relay.RelayedHttpListenerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalHttpResponseTest {

  private LocalHttpResponse localHttpResponse;

  private final String ERR_MSG = "Error Message";

  @Mock private Exception exception;

  @Mock private RelayedHttpListenerContext context;

  @BeforeEach
  void setUp() {
    when(exception.getMessage()).thenReturn(ERR_MSG);
  }

  @Test
  void createErrorLocalHttpResponse_containsExceptionMessageAndStatusCode() {
    localHttpResponse = LocalHttpResponse.createErrorLocalHttpResponse(500, exception, context);
    assertThat(localHttpResponse.getStatusCode(), equalTo(500));
    assertThat(localHttpResponse.getStatusDescription(), equalTo(ERR_MSG));
  }
}
