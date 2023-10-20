package it.gov.pagopa.common.reactive.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DummySpringRepository extends ReactiveMongoRepository<DummySpringRepository.DummyMongoCollection, String> {
    Mono<DummyMongoCollection> findByIdOrderById(String id);
    Flux<DummyMongoCollection> findByIdOrderByIdDesc(String id);

    @Document("beneficiary_rule")
    @Data
    class DummyMongoCollection {
        @Id
        private String id;
    }
}
