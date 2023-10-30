package it.gov.pagopa.common.mongo.singleinstance;

import de.flapdoodle.embed.mongo.spring.autoconfigure.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;

import java.lang.annotation.*;

/** It will enable the usage of a single instance of {@link EmbeddedMongoAutoConfiguration}, dropping the database at each new Spring Context */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AutoConfigureDataMongo
@ImportAutoConfiguration(exclude = EmbeddedMongoAutoConfiguration.class)
public @interface AutoConfigureSingleInstanceMongodb {
}
