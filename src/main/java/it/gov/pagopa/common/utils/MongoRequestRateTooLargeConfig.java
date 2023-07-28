package it.gov.pagopa.common.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MongoRequestRateTooLargeConfig {

  @SuppressWarnings("squid:S1444")
  public static int maxRetry;

  @SuppressWarnings("squid:S1444")
  public static int maxMillisElapsed;

  public MongoRequestRateTooLargeConfig(
      @Value("${mongo.request-rate-too-large.max-retry}") int configuredMaxRetry,
      @Value("${mongo.request-rate-too-large.max-millis-elapsed}") int configuredMaxMillisElapsed) {

    maxRetry = configuredMaxRetry;
    maxMillisElapsed = configuredMaxMillisElapsed;
  }

}