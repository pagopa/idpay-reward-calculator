package it.gov.pagopa.common.reactive.mongo;

import com.mongodb.client.result.DeleteResult;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.NoRepositoryBean;
import reactor.core.publisher.Mono;

@NoRepositoryBean
public interface ReactiveMongoRepositoryExt<T, I> extends ReactiveMongoRepository<T, I> {
    Mono<DeleteResult> removeById(I id);
}
