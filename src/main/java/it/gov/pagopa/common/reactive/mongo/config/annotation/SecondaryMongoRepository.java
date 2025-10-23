package it.gov.pagopa.common.reactive.mongo.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotazione marker da applicare ai repository che devono usare il database secondario Mongo.
 * Esempio:
 * @SecondaryMongoRepository
 * public interface MyRepo extends ReactiveMongoRepository<MyDoc,String> {}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SecondaryMongoRepository {
}

