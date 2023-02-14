package it.gov.pagopa.reward.service.build;

import it.gov.pagopa.reward.model.DroolsRule;
import org.kie.api.KieBase;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * It will handle the compilation operation to obtain a new KieContainer
 * */
public interface KieContainerBuilderService {
    /**
     * It will fetch all the {@link DroolsRule} entity , build and new KieContainer
     * */
    Mono<KieBase> buildAll();
    /**
     * It will compile a Flux of {@link DroolsRule} entities returning a new KieContainer when the input Flux completes
     * */
    Mono<KieBase> build(Flux<DroolsRule> rules);

    /** It will preload all the initiatives rules putting them in execution */
    void preLoadKieBase(KieBase kieBase);
}
