package it.gov.pagopa.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

@Slf4j(topic = "AUDIT")
public class AuditLogger {
    private AuditLogger() {
    }

    public static final String SRCIP;

    static {
        String srcIp;
        try {
            srcIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("Cannot determine the ip of the current host", e);
            srcIp = "UNKNOWN";
        }

        SRCIP = srcIp;
    }

    public static void logAuditString(String pattern, String... parameters) {
        Object[] params = Arrays.asList(parameters).toArray();
        log.info(pattern,params);
    }

}