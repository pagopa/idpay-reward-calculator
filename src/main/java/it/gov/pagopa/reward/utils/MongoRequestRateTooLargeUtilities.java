package it.gov.pagopa.reward.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MongoRequestRateTooLargeUtilities {

  @SuppressWarnings("squid:S1444")
  public static int maxRetry;
  @SuppressWarnings("squid:S1444")
  public static int maxMillisElapsed;

  @SuppressWarnings("squid:S1118")
  public MongoRequestRateTooLargeUtilities(
      @Value("${mongo.request-rate-too-large.max-retry}") int retry,
      @Value("${mongo.request-rate-too-large.max-millis-elapsed}") int millisElapsed) {

    maxRetry = retry;
    maxMillisElapsed = millisElapsed;
  }

}