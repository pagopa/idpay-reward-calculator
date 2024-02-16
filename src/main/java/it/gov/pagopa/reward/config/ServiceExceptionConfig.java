package it.gov.pagopa.reward.config;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.reward.exception.custom.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ServiceExceptionConfig {

    @Bean
    public Map<Class<? extends ServiceException>, HttpStatus> serviceExceptionMapper() {
        Map<Class<? extends ServiceException>, HttpStatus> exceptionMap = new HashMap<>();

        // ClientError
        exceptionMap.put(InvalidCounterVersionException.class, HttpStatus.PRECONDITION_FAILED);

        // Forbidden
        exceptionMap.put(InitiativeNotActiveException.class, HttpStatus.FORBIDDEN);

        // NotFound
        exceptionMap.put(InitiativeNotFoundOrNotDiscountException.class, HttpStatus.NOT_FOUND);

        // TooManyRequests
        exceptionMap.put(InitiativeNotInContainerException.class, HttpStatus.TOO_MANY_REQUESTS);

        //Locked
        exceptionMap.put(PendingCounterException.class, HttpStatus.LOCKED);

        return exceptionMap;
    }
}
