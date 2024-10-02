package it.gov.pagopa.common.config;

import com.mongodb.MongoException;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;


class CustomMongoHealthIndicatorTest {
    @Test
    void testMongoIsUp() {
        Document buildInfo = mock(Document.class);
        given(buildInfo.getInteger("maxWireVersion")).willReturn(10);
        ReactiveMongoTemplate reactiveMongoTemplate = mock(ReactiveMongoTemplate.class);
        given(reactiveMongoTemplate.executeCommand("{ isMaster: 1 }")).willReturn(Mono.just(buildInfo));
        CustomReactiveMongoHealthIndicator CustomReactiveMongoHealthIndicator = new CustomReactiveMongoHealthIndicator(
                reactiveMongoTemplate);
        Mono<Health> health = CustomReactiveMongoHealthIndicator.health();
        StepVerifier.create(health).consumeNextWith((h) -> {
            assertThat(h.getStatus()).isEqualTo(Status.UP);
            assertThat(h.getDetails()).containsOnlyKeys("maxWireVersion");
            assertThat(h.getDetails()).containsEntry("maxWireVersion", 10);
        }).expectComplete().verify(Duration.ofSeconds(30));
    }

    @Test
    void testMongoIsDown() {
        ReactiveMongoTemplate reactiveMongoTemplate = mock(ReactiveMongoTemplate.class);
        given(reactiveMongoTemplate.executeCommand("{ isMaster: 1 }")).willThrow(new MongoException("Connection failed"));
        CustomReactiveMongoHealthIndicator CustomReactiveMongoHealthIndicator = new CustomReactiveMongoHealthIndicator(
                reactiveMongoTemplate);
        Mono<Health> health = CustomReactiveMongoHealthIndicator.health();
        StepVerifier.create(health).consumeNextWith((h) -> {
            assertThat(h.getStatus()).isEqualTo(Status.DOWN);
            assertThat(h.getDetails()).containsOnlyKeys("error");
            assertThat(h.getDetails()).containsEntry("error", MongoException.class.getName() + ": Connection failed");
        }).expectComplete().verify(Duration.ofSeconds(30));
    }

}
