package it.gov.pagopa.common.config;

import com.mongodb.MongoException;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;


class CustomMongoHealthIndicatorTest {


    @Test
    void mongoIsUp() {
        Document commandResult = mock(Document.class);
        given(commandResult.getInteger("maxWireVersion")).willReturn(10);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        given(mongoTemplate.executeCommand("{ isMaster: 1 }")).willReturn(commandResult);
        CustomMongoHealthIndicator healthIndicator = new CustomMongoHealthIndicator(mongoTemplate);
        Health health = healthIndicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("maxWireVersion", 10);
        then(commandResult).should().getInteger("maxWireVersion");
        then(mongoTemplate).should().executeCommand("{ isMaster: 1 }");
    }

    @Test
    void mongoIsDown() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        given(mongoTemplate.executeCommand("{ isMaster: 1 }")).willThrow(new MongoException("Connection failed"));
        CustomMongoHealthIndicator healthIndicator = new CustomMongoHealthIndicator(mongoTemplate);
        Health health = healthIndicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat((String) health.getDetails().get("error")).contains("Connection failed");
        then(mongoTemplate).should().executeCommand("{ isMaster: 1 }");
    }
}
