package it.gov.pagopa.common.mongo;

public interface EmbeddedMongodbTestClient {
    void dropDatabase(String mongodbUrl, String dbName);
}
