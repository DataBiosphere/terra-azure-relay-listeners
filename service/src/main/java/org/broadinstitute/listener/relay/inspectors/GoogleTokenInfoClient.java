package org.broadinstitute.listener.relay.inspectors;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class GoogleTokenInfoClient {
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final String GOOGLE_OAUTH_SERVER =
      "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=";

  public GoogleOauthInfoResponse getTokenInfo(String token)
      throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder().uri(URI.create(GOOGLE_OAUTH_SERVER + token)).build();

    var oauthInfoResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return new Gson().fromJson(oauthInfoResponse.body(), GoogleOauthInfoResponse.class);
  }
}
