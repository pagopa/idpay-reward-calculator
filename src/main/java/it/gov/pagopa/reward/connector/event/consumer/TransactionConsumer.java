package it.gov.pagopa.reward.connector.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.service.RewardErrorNotifierService;
import it.gov.pagopa.reward.service.counters.UserInitiativeCountersUnlockMediatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
public class TransactionConsumer extends BaseKafkaConsumer<RewardTransactionDTO, UserInitiativeCounters>  {

    private final RewardErrorNotifierService rewardErrorNotifierService;
    private final UserInitiativeCountersUnlockMediatorService userInitiativeCountersUnlockMediatorService;
    private final Duration commitDelay;
    private final ObjectReader objectReader;

    public TransactionConsumer(@Value("${spring.application.name}") String applicationName,
                               RewardErrorNotifierService rewardErrorNotifierService,
                               UserInitiativeCountersUnlockMediatorService userInitiativeCountersUnlockMediatorService,
                               @Value("${spring.cloud.stream.kafka.bindings.trxResponseConsumer-in-0.consumer.ackTime}") long commitMillis,
                               ObjectMapper objectMapper){
        super(applicationName);
        this.rewardErrorNotifierService = rewardErrorNotifierService;
        this.userInitiativeCountersUnlockMediatorService = userInitiativeCountersUnlockMediatorService;
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
        rewardErrorNotifierService.notifyTransactionResponse(message, "[USER_COUNTER_UNLOCK] An error occurred evaluating transaction", true, e);

    }

    @Override
    protected Mono<UserInitiativeCounters> execute(RewardTransactionDTO payload, Message<String> message, Map<String, Object> ctx) {
        return userInitiativeCountersUnlockMediatorService.execute(payload);
    }

}
