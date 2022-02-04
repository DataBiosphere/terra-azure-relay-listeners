package org.broadinstitute.listener.relay.http;

import com.microsoft.azure.relay.RelayedHttpListenerContext;
import com.microsoft.azure.relay.RelayedHttpListenerResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class RelayedHttpRequestProcessor {

  private final HttpClient httpClient;
  protected final Logger logger = LoggerFactory.getLogger(RelayedHttpRequestProcessor.class);

  public RelayedHttpRequestProcessor(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public RelayedHttpRequestProcessor() {
    this(HttpClient.newBuilder().build());
  }

  public LocalHttpResponse executeLocalRequest(RelayedHttpRequest request) {

    HttpResponse<?> clientResponse = null;
    try {

      HttpRequest localRequest = toClientHttpRequest(request);

      clientResponse = httpClient.send(localRequest, HttpResponse.BodyHandlers.ofInputStream());

      return LocalHttpResponse.createLocalHttpResponse(clientResponse, request.getContext());

    } catch (Exception ex) {

      if (clientResponse != null && clientResponse.body() != null) {
        try {
          ((InputStream) clientResponse.body()).close();
        } catch (IOException e) {
          logger.error("Failed to close body from response.", ex);
        }
      }
      return handleExceptionResponse(ex, request.getContext());
    }
  }

  public Result writeLocalResponse(@NonNull LocalHttpResponse localResponse) {
    RelayedHttpListenerResponse listenerResponse = localResponse.getContext().getResponse();

    listenerResponse.setStatusCode(localResponse.getStatusCode());

    if (StringUtils.isNotBlank(localResponse.getStatusDescription())) {
      listenerResponse.setStatusDescription(localResponse.getStatusDescription());
    }

    if (localResponse.getHeaders().isPresent()) {
      listenerResponse.getHeaders().putAll(localResponse.getHeaders().get());
    }

    if (localResponse.getBody().isPresent()) {
      try {
        listenerResponse.getOutputStream().write(localResponse.getBody().get().readAllBytes());
        localResponse.getBody().get().close();
        listenerResponse.getOutputStream().close();
      } catch (IOException e) {
        logger.error("Failed to write response body to the remote client.", e);
        return Result.FAILURE;
      }
    }
    return Result.SUCCESS;
  }

  private LocalHttpResponse handleExceptionResponse(
      Exception exception, RelayedHttpListenerContext context) {
    logger.error("Relay request failed.", exception);
    return LocalHttpResponse.createErrorLocalHttpResponse(500, exception, context);
  }

  private HttpRequest toClientHttpRequest(RelayedHttpRequest request)
      throws URISyntaxException, MalformedURLException {
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(request.getTargetURL().toURI());

    HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();

    if (request.getBody().isPresent()) {
      bodyPublisher = HttpRequest.BodyPublishers.ofInputStream(() -> request.getBody().get());
    }
    requestBuilder.method(request.getMethod(), bodyPublisher);

    logger.debug("Constructing local HTTP Request. URI: {}", request.getTargetURL());

    if (request.getHeaders().isPresent()) {
      for (Map.Entry<String, String> entry : request.getHeaders().get().entrySet()) {
        String key = entry.getKey();

        // TODO:Implement masking for sensitive values.
        logger.debug("Header name:{} value:{}", entry.getKey(), entry.getValue());

        // These headers cannot be set in Java http client
        if (key.equals("Host") || key.equals("Via")) {
          continue;
        }

        String value = entry.getValue();
        requestBuilder.header(key, value);
      }
    }

    return requestBuilder.build();
  }

  public enum Result {
    SUCCESS,
    FAILURE
  }
}
