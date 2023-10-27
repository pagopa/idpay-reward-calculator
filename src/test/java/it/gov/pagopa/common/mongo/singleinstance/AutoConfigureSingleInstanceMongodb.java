package it.gov.pagopa.common.mongo.singleinstance;

import de.flapdoodle.embed.mongo.spring.autoconfigure.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/** It will enable the usage of a single instance of {@link EmbeddedMongoAutoConfiguration}, dropping the database at each new Spring Context */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AutoConfigureDataMongo
@EnableAutoConfiguration(exclude = EmbeddedMongoAutoConfiguration.class)
@Import(SingleEmbeddedMongodbConfiguration.class)
public @interface AutoConfigureSingleInstanceMongodb {
}
