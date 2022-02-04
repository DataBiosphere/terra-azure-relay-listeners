package org.broadinstitute.listener.relay.http;

import com.microsoft.azure.relay.RelayedHttpListenerResponse;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class LocalHttpResponseWriter {

  protected final Logger logger = LoggerFactory.getLogger(LocalHttpResponseWriter.class);

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

  public enum Result {
    SUCCESS,
    FAILURE
  }
}
