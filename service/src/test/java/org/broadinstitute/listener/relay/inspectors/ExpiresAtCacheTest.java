package org.broadinstitute.listener.relay.inspectors;

import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
class ExpiresAtCacheTest {
  @Autowired private CacheManager cacheManager;

  private SamResourceClient mock;

  @Autowired private SamResourceClient samResourceClient;

  @Configuration
  @EnableCaching
  public static class CacheTestConfig {
    @Bean
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("expiresAt");
    }

    @Bean
    public SamResourceClient samResourceClient() {
      return mock(SamResourceClient.class);
    }
  }

  @BeforeEach
  void setUp() {
    mock = AopTestUtils.getTargetObject(samResourceClient);
    reset(mock);
    when(mock.checkPermission("accessToken")).thenReturn(Instant.ofEpochSecond(100));
  }

  private Optional<Instant> getCachedExpiresAt(String accessToken) {
    return ofNullable(cacheManager.getCache("expiresAt"))
        .map(c -> c.get(accessToken, Instant.class));
  }

  @Test
  void checkWritePermission_success() {
    samResourceClient.checkPermission("accessToken");
    assertThat(getCachedExpiresAt("accessToken").get(), equalTo(Instant.ofEpochSecond(100)));
    assertEquals(getCachedExpiresAt("accessToken-non-existent"), Optional.empty());
    verify(mock, times(1)).checkPermission("accessToken");

    samResourceClient.checkPermission("accessToken");
    verifyNoMoreInteractions(mock);

    samResourceClient.checkPermission("accessToken2");
    verify(mock, times(1)).checkPermission("accessToken2");
  }
}
