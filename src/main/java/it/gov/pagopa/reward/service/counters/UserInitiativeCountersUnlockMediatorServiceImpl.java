package it.gov.pagopa.reward.service.counters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.service.RewardErrorNotifierService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static it.gov.pagopa.reward.utils.RewardConstants.REWARD_STATE_AUTHORIZED;

@Service
@Slf4j
public class UserInitiativeCountersUnlockMediatorServiceImpl extends BaseKafkaConsumer<RewardTransactionDTO, UserInitiativeCounters>  implements UserInitiativeCountersUnlockMediatorService {

    private static final List<String> ACCEPTED_STATUS = List.of(REWARD_STATE_AUTHORIZED);
    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final RewardErrorNotifierService rewardErrorNotifierService;
    private final Duration commitDelay;
    private final ObjectReader objectReader;

    public UserInitiativeCountersUnlockMediatorServiceImpl(@Value("${spring.application.name}") String applicationName,
                                                           UserInitiativeCountersRepository userInitiativeCountersRepository,
                                                           RewardErrorNotifierService rewardErrorNotifierService,
                                                           @Value("${spring.cloud.stream.kafka.bindings.trxResponseConsumer-in-0.consumer.ackTime}") long commitMillis,
                                                           ObjectMapper objectMapper){
        super(applicationName);
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.rewardErrorNotifierService = rewardErrorNotifierService;
        this.commitDelay = Duration.ofMillis(commitMillis);
        this.objectReader = objectMapper.readerFor(RewardTransactionDTO.class);

    }
    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<UserInitiativeCounters>> afterCommits2subscribe) {
        afterCommits2subscribe.subscribe(updateResults -> log.info("[USER_COUNTER_UNLOCK] Processed offsets committed successfully"));
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> rewardErrorNotifierService.notifyTransactionResponse(message, "[USER_COUNTER_UNLOCK] Unexpected JSON", true, e);

    }

    @Override
    public String getFlowName() {
        return "USER_COUNTER_UNLOCK";
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {
        rewardErrorNotifierService.notifyTransactionResponse(message, "[USER_COUNTER_UNLOCK] An error occurred evaluating hpan update", false, e);

    }

    @Override
    protected Mono<UserInitiativeCounters> execute(RewardTransactionDTO payload, Message<String> message, Map<String, Object> ctx) {
        log.info("[USER_COUNTER_UNLOCK] Started processing transaction response " + payload);
        return Mono.just(payload)
                .filter(trx -> ACCEPTED_STATUS.contains(trx.getStatus()))
                .flatMap(this::handlerUnlockType);
    }

    private Mono<UserInitiativeCounters> handlerUnlockType(RewardTransactionDTO trx) {
        if(REWARD_STATE_AUTHORIZED.equals(trx.getStatus())) {
            log.info("[USER_COUNTER_UNLOCK] Started processing transaction in status AUTHORIZED");
            return userInitiativeCountersRepository.unlockPendingTrx(trx.getId());
        }
        //TODO handle expired event (CANCELED status)
        return Mono.empty();
    }
}
