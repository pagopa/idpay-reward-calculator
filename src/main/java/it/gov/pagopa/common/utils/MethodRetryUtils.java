package it.gov.pagopa.common.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodRetryUtils {
    private MethodRetryUtils(){}

    public static void exec(String desc, Runnable task, int maxRetries) {
        execInner(desc, task, 0, maxRetries);
    }
    private static void execInner(String desc, Runnable task, int attempt, int maxRetries) {
        try{
            task.run();
        } catch (Exception e){

            if(attempt >= maxRetries){
                log.error("Maximum attempts for task {} has been reached! attempt {} of {}", desc, attempt, maxRetries);
                throw e;
            } else {
                log.info("Retrying task {}: attempt {} of {}", desc, attempt, maxRetries);
                execInner(desc, task, attempt+1, maxRetries);
            }
        }
    }
}
