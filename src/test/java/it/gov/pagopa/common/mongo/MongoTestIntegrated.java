package it.gov.pagopa.common.mongo;

import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * See confluence page: <a href="https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/615974424/Secrets+UnitTests">Secrets for UnitTests</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@TestPropertySource(
        properties = {
                "spring.autoconfigure.exclude=" +
                        "  de.flapdoodle.embed.mongo.spring.autoconfigure.EmbeddedMongoAutoConfiguration," +
                        "  it.gov.pagopa.common.mongo.singleinstance.SingleEmbeddedMongodbAutoConfiguration"
        },
        locations = {
                "classpath:/secrets/mongodbConnectionString.properties"
        })
public @interface MongoTestIntegrated {
}
