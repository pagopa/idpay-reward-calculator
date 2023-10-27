package it.gov.pagopa.common.reactive.mongo;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import it.gov.pagopa.common.mongo.EmbeddedMongodbTestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.BaseSubscriber;

@Slf4j
@Service
public class ReactiveEmbeddedMongodbTestClient implements EmbeddedMongodbTestClient {
    @Override
    public void dropDatabase(String mongodbUrl, String dbName) {
        try(MongoClient mongoClient = MongoClients.create(mongodbUrl)){
            mongoClient.getDatabase(dbName).drop().subscribe(new BaseSubscriber<>() {
                @Override
                protected void hookOnComplete() {
                    log.info("Database {} dropped", dbName);
                    mongoClient.notify();
                }
            });
            synchronized (mongoClient){
                mongoClient.wait(1000);
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("Cannot drop database", e);
        }
    }
}
