package it.gov.pagopa.common.reactive.kafka.utils;

public class KafkaConstants {
    private KafkaConstants(){}

//region error-topic headers
    public static final String ERROR_MSG_HEADER_APPLICATION_NAME = "applicationName";
    public static final String ERROR_MSG_HEADER_GROUP = "group";
    public static final String ERROR_MSG_HEADER_SRC_TYPE = "srcType";
    public static final String ERROR_MSG_HEADER_SRC_SERVER = "srcServer";
    public static final String ERROR_MSG_HEADER_SRC_TOPIC = "srcTopic";
    public static final String ERROR_MSG_HEADER_DESCRIPTION = "description";
    public static final String ERROR_MSG_HEADER_RETRY = "retry";
    public static final String ERROR_MSG_HEADER_RETRYABLE = "retryable";
    public static final String ERROR_MSG_HEADER_STACKTRACE = "stacktrace";
//endregion
}
