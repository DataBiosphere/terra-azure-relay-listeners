package org.broadinstitute.listener.relay.http;

import static com.google.common.net.HttpHeaders.SET_COOKIE;

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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.listener.config.CorsSupportProperties;
import org.broadinstitute.listener.relay.Utils;
import org.broadinstitute.listener.relay.inspectors.HeaderLoggerInspector;
import org.broadinstitute.listener.relay.inspectors.TokenChecker;
import org.broadinstitute.listener.relay.transport.TargetResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

public class RelayedHttpRequestProcessor {

  private final HttpClient httpClient;
  private final TargetResolver targetHostResolver;
  private final CorsSupportProperties corsSupportProperties;

  private final TokenChecker tokenChecker;

  protected final Logger logger = LoggerFactory.getLogger(RelayedHttpRequestProcessor.class);

  public RelayedHttpRequestProcessor(
      @NonNull TargetResolver targetHostResolver,
      CorsSupportProperties corsSupportProperties,
      TokenChecker tokenChecker) {
    this.httpClient = HttpClient.newBuilder().version(Version.HTTP_1_1).build();
    this.targetHostResolver = targetHostResolver;
    this.corsSupportProperties = corsSupportProperties;
    this.tokenChecker = tokenChecker;
  }

  public RelayedHttpRequestProcessor(
      HttpClient httpClient,
      @NonNull TargetResolver targetHostResolver,
      CorsSupportProperties corsSupportProperties,
      TokenChecker tokenChecker) {
    this.httpClient = httpClient;
    this.targetHostResolver = targetHostResolver;
    this.corsSupportProperties = corsSupportProperties;
    this.tokenChecker = tokenChecker;
  }

  public TargetHttpResponse executeRequestOnTarget(RelayedHttpListenerContext requestContext) {

    HttpResponse<?> clientResponse = null;
    try {
      RelayedHttpRequest request =
          RelayedHttpRequest.createRelayedHttpRequest(requestContext, targetHostResolver);

      HttpRequest localRequest = toClientHttpRequest(request);

      logger.debug("Local request: {}", localRequest.uri().toString());

      clientResponse = httpClient.send(localRequest, HttpResponse.BodyHandlers.ofInputStream());

      return TargetHttpResponse.createTargetHttpResponse(
          clientResponse, request.getContext(), corsSupportProperties);

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
    Utils.writeCORSHeaders(
        listenerResponse.getHeaders(), context.getRequest().getHeaders(), corsSupportProperties);

    try {
      listenerResponse.getOutputStream().close();
    } catch (IOException e) {
      logger.error("Failed to close response body to the remote client.", e);
      return Result.FAILURE;
    }
    return Result.SUCCESS;
  }

  public Result writeSetCookieResponse(RelayedHttpListenerContext context) {
    if (context.getResponse() == null) {
      logger.error("The context did not have a valid response");
      return Result.FAILURE;
    }

    var authToken = Utils.getTokenFromAuthorization(context.getRequest().getHeaders());
    if (authToken.isEmpty()) return Result.FAILURE;
    else {
      try {
        var oauthInfo = tokenChecker.getOauthInfo(authToken.get());

        var now = Instant.now();
        Optional<Long> expiresIn =
            oauthInfo.expiresAt().map(i -> i.getEpochSecond() - now.getEpochSecond());

        RelayedHttpListenerResponse listenerResponse = context.getResponse();
        listenerResponse.setStatusCode(204);

        listenerResponse
            .getHeaders()
            .put(
                SET_COOKIE,
                String.format(
                    "%s=%s; Max-Age=%s; Path=/; Secure; SameSite=None",
                    Utils.TOKEN_NAME, authToken.get(), expiresIn.orElse(0L)));

        Utils.writeCORSHeaders(
            listenerResponse.getHeaders(),
            context.getRequest().getHeaders(),
            corsSupportProperties);

        listenerResponse.getOutputStream().close();
      } catch (IOException e) {
        logger.error("Failed to close response body to the remote client.", e);
        return Result.FAILURE;
      } catch (InterruptedException e) {
        logger.error("Fail to Decode token", e);
        return Result.FAILURE;
      }
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

      removeHeadersNotAcceptedByAzureRelay(listenerResponse.getHeaders());
    }

    var foo = new HeaderLoggerInspector();
    foo.logRequest(
        targetResponse.getContext().getRequest(),
        targetResponse.getContext().getResponse(),
        OffsetDateTime.now(),
        "LOGGING_TEST");

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

  private void removeHeadersNotAcceptedByAzureRelay(Map<String, String> headers) {
    headers.remove("transfer-encoding");
    headers.remove("Transfer-Encoding");
  }

  public TargetHttpResponse handleExceptionResponse(
      Throwable exception, RelayedHttpListenerContext context) {
    String message =
        String.format(
            Locale.ROOT,
            "Relayed request failed. Tracking ID:%s",
            context.getTrackingContext().getTrackingId());
    logger.error(message, exception);
    return TargetHttpResponse.createTargetHttpResponseFromException(
        500, exception, context, corsSupportProperties);
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
