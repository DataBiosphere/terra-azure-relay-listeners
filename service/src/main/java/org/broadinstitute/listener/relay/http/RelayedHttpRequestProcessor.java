package org.broadinstitute.listener.relay.http;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_MAX_AGE;

import com.microsoft.azure.relay.RelayedHttpListenerContext;
import com.microsoft.azure.relay.RelayedHttpListenerResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.listener.config.CorsSupportProperties;
import org.broadinstitute.listener.relay.transport.TargetResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

public class RelayedHttpRequestProcessor {

  private final HttpClient httpClient;
  private final TargetResolver targetHostResolver;
  private final CorsSupportProperties corsSupportProperties;

  protected final Logger logger = LoggerFactory.getLogger(RelayedHttpRequestProcessor.class);

  public RelayedHttpRequestProcessor(
      @NonNull TargetResolver targetHostResolver, CorsSupportProperties corsSupportProperties) {
    this.httpClient = HttpClient.newBuilder().version(Version.HTTP_1_1).build();
    this.targetHostResolver = targetHostResolver;
    this.corsSupportProperties = corsSupportProperties;
  }

  public RelayedHttpRequestProcessor(
      HttpClient httpClient,
      @NonNull TargetResolver targetHostResolver,
      CorsSupportProperties corsSupportProperties) {
    this.httpClient = httpClient;
    this.targetHostResolver = targetHostResolver;
    this.corsSupportProperties = corsSupportProperties;
  }

  public TargetHttpResponse executeRequestOnTarget(RelayedHttpListenerContext requestContext) {

    HttpResponse<?> clientResponse = null;
    try {
      RelayedHttpRequest request =
          RelayedHttpRequest.createRelayedHttpRequest(requestContext, targetHostResolver);

      HttpRequest localRequest = toClientHttpRequest(request);

      clientResponse = httpClient.send(localRequest, HttpResponse.BodyHandlers.ofInputStream());

      return TargetHttpResponse.createTargetHttpResponse(clientResponse, request.getContext());

    } catch (Throwable ex) {

      if (clientResponse != null && clientResponse.body() != null) {
        try {
          ((InputStream) clientResponse.body()).close();
        } catch (IOException e) {
          logger.error("Failed to close body from response.", ex);
        }
      }
      return handleExceptionResponse(ex, requestContext);
    }
  }

  public Result writeNotAcceptedResponseOnCaller(RelayedHttpListenerContext context) {
    if (context.getResponse() == null) {
      logger.error("The context did not have a valid response");
      return Result.FAILURE;
    }

    RelayedHttpListenerResponse listenerResponse = context.getResponse();
    String msg =
        String.format(
            Locale.ROOT,
            "The listener rejected the request. Tracking ID:%s",
            context.getTrackingContext().getTrackingId());
    listenerResponse.setStatusCode(403);
    listenerResponse.setStatusDescription(msg);
    try {
      listenerResponse.getOutputStream().close();
    } catch (IOException e) {
      logger.error("Failed to close response body to the remote client.", e);
    }
    return Result.FAILURE;
  }

  public Result writePreflightResponse(RelayedHttpListenerContext context) {
    if (context.getResponse() == null) {
      logger.error("The context did not have a valid response");
      return Result.FAILURE;
    }

    RelayedHttpListenerResponse listenerResponse = context.getResponse();
    listenerResponse.setStatusCode(204);
    listenerResponse
        .getHeaders()
        .put(ACCESS_CONTROL_ALLOW_METHODS, corsSupportProperties.preflightMethods());

    listenerResponse
        .getHeaders()
        .put(
            ACCESS_CONTROL_ALLOW_ORIGIN,
            context.getRequest().getHeaders().getOrDefault("Origin", "*"));

    listenerResponse.getHeaders().put(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    listenerResponse
        .getHeaders()
        .put(ACCESS_CONTROL_ALLOW_HEADERS, corsSupportProperties.allowHeaders());
    listenerResponse.getHeaders().put(ACCESS_CONTROL_MAX_AGE, corsSupportProperties.maxAge());
    try {
      listenerResponse.getOutputStream().close();
    } catch (IOException e) {
      logger.error("Failed to close response body to the remote client.", e);
      return Result.FAILURE;
    }
    return Result.SUCCESS;
  }

  public Result writeTargetResponseOnCaller(@NonNull TargetHttpResponse targetResponse) {

    if (targetResponse.getContext().getResponse() == null) {
      logger.error("The context did not have a valid response");
      return Result.FAILURE;
    }

    RelayedHttpListenerResponse listenerResponse = targetResponse.getContext().getResponse();

    listenerResponse.setStatusCode(targetResponse.getStatusCode());

    if (StringUtils.isNotBlank(targetResponse.getStatusDescription())) {
      listenerResponse.setStatusDescription(targetResponse.getStatusDescription());
    }

    if (targetResponse.getHeaders().isPresent()) {
      listenerResponse.getHeaders().putAll(targetResponse.getHeaders().get());
    }
    OutputStream outputStream = targetResponse.getCallerResponseOutputStream();

    Result result = Result.SUCCESS;
    if (targetResponse.getBody().isPresent()) {
      try {
        outputStream.write(targetResponse.getBody().get().readAllBytes());
      } catch (IOException e) {
        logger.error("Failed to write response body to the remote client.", e);
        result = Result.FAILURE;
      }

      try {
        targetResponse.getBody().get().close();
      } catch (IOException e) {
        logger.error("Failed to close target response.", e);
        result = Result.FAILURE;
      }
    }

    try {
      outputStream.close();
    } catch (IOException e) {
      logger.error("Failed to close caller response.", e);
      result = Result.FAILURE;
    }

    return result;
  }

  public TargetHttpResponse handleExceptionResponse(
      Throwable exception, RelayedHttpListenerContext context) {
    String message =
        String.format(
            Locale.ROOT,
            "Relayed request failed. Tracking ID:%s",
            context.getTrackingContext().getTrackingId());
    logger.error(message, exception);
    return TargetHttpResponse.createTargetHttpResponseFromException(500, exception, context);
  }

  private HttpRequest toClientHttpRequest(RelayedHttpRequest request) throws URISyntaxException {
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(request.getTargetUrl().toURI());

    HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();

    if (request.getBody().isPresent()) {
      bodyPublisher = HttpRequest.BodyPublishers.ofInputStream(() -> request.getBody().get());
    }
    requestBuilder.method(request.getMethod(), bodyPublisher);

    logger.debug("Constructing local HTTP Request. URI: {}", request.getTargetUrl());

    if (request.getHeaders().isPresent()) {
      for (Map.Entry<String, String> entry : request.getHeaders().get().entrySet()) {
        String key = entry.getKey();

        // Not logging values as they could be sensitive.
        // TODO: implement logging for values that masks sensitive information.
        logger.debug("Header name:{}", entry.getKey());

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
