package it.gov.pagopa.common.reactive.mongo;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import it.gov.pagopa.common.mongo.EmbeddedMongodbTestClient;
import it.gov.pagopa.common.mongo.singleinstance.SingleInstanceMongodWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.BaseSubscriber;

import java.util.Objects;

@Slf4j
@Service
public class EmbeddedMongodbTestReactiveClient implements EmbeddedMongodbTestClient {

    private final String dbName;

    public EmbeddedMongodbTestReactiveClient(Environment env) {
        this.dbName = Objects.requireNonNull(env.getProperty("spring.data.mongodb.database"));
    }

    @Override
    public void dropDatabase() {
        String mongodbUrl = getEmbeddedMongdbUrl();

        try(MongoClient mongoClient = MongoClients.create(mongodbUrl)){
            mongoClient.getDatabase(dbName).drop().subscribe(new BaseSubscriber<>() {
                @Override
                protected void hookOnComplete() {
                    log.info("Database {} dropped", dbName);
                    synchronized (mongoClient){
                        mongoClient.notify();
                    }
                }
            });
            synchronized (mongoClient){
                mongoClient.wait(1000);
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("Cannot drop database", e);
        }
    }

    private static String getEmbeddedMongdbUrl() {
        int dbPort = Objects.requireNonNull(SingleInstanceMongodWrapper.singleMongodNet).getPort();
        return "mongodb://localhost:" + dbPort;
    }
}
