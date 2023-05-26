package org.broadinstitute.listener.relay.inspectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class RequestLoggerTest {

  @Mock RelayedHttpListenerRequest mockRequest;

  @Test
  void logRequest() throws URISyntaxException, IOException, InterruptedException {
    Logger headerLogger = (Logger) LoggerFactory.getLogger(RequestLogger.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    headerLogger.addAppender(appender);

    var endpoint = new InetSocketAddress("127.0.0.1", 65535);
    when(mockRequest.getHttpMethod()).thenReturn("GET");
    when(mockRequest.getUri())
        .thenReturn(new URI("sb://lzexample.servicebus.windows.net/wds-example-example"));
    var token = TokenUtils.buildJWT(Instant.now().plusSeconds(120));
    when(mockRequest.getHeaders())
        .thenReturn(
            Map.of(
                "Authorization",
                "Bearer " + token,
                "ReFeRer", // make sure we handle case-insensitivity
                "http://example.com/referer",
                "User-Agent",
                "Mozilla/5.0 (Macintosh Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML,like Gecko) Chrome/113.0.0.0 Safari/537.36"));
    when(mockRequest.getRemoteEndPoint()).thenReturn(endpoint);

    var requestLogger = new RequestLogger();

    requestLogger.logRequest(
        mockRequest, 200, OffsetDateTime.parse("2023-05-23T10:23:22.256527-04:00"), "HTTP_REQUEST");

    var msgs = appender.list.stream().filter(i -> i.getLevel().equals(Level.INFO)).toList();
    assertThat(
        msgs.get(0).getFormattedMessage(),
        equalTo(
            "HTTP_REQUEST 127.0.0.1 123ABC \"example@example.com\" [2023-05-23T10:23:22.256527-04:00] app \"GET sb://lzexample.servicebus.windows.net/wds-example-example\" 200 \"http://example.com/referer\" \"-\" \"Mozilla/5.0 (Macintosh Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML,like Gecko) Chrome/113.0.0.0 Safari/537.36\""));
  }
}
