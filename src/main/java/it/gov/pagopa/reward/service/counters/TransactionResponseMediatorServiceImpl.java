package it.gov.pagopa.reward.service.counters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
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

import static it.gov.pagopa.reward.utils.RewardConstants.REWARD_STATE_AUTHORIZED;

@Service
@Slf4j
public class TransactionResponseMediatorServiceImpl extends BaseKafkaConsumer<RewardTransactionDTO, String>  implements  TransactionResponseMediatorService{

    private final List<String> ACCEPTED_STATUS = List.of(REWARD_STATE_AUTHORIZED);
    private final Duration commitDelay;
    private final ObjectReader objectReader;

    public TransactionResponseMediatorServiceImpl(@Value("${spring.application.name}") String applicationName,
                                                  @Value("${sspring.cloud.stream.kafka.bindings.trxResponseConsumer-in-0.consumer.ackTime}") long commitMillis,
                                                  ObjectMapper objectMapper){
        super(applicationName);
        this.commitDelay = Duration.ofMillis(commitMillis);
        this.objectReader = objectMapper.readerFor(RewardTransactionDTO.class);

    }
    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<String>> afterCommits2subscribe) {

    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> rewardErrorNotifierService.notifyTransactionEvaluation(message, "[USER_COUNTER_UNLOCK] Unexpected JSON", true, e); //TODO Change with new method on service

    }

    @Override
    public String getFlowName() {
        return "USER_COUNTER_UNLOCK";
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {

    }

    @Override
    protected Mono<String> execute(RewardTransactionDTO payload, Message<String> message, Map<String, Object> ctx) {
        /*todo
        *  filter for status authorized
        *  ritrovamento del contatore
        *  cancellazione pendingTrx, controllando il solo trxId
        * */
        return Mono.just(payload)
                .filter(trx -> ACCEPTED_STATUS.contains(trx.getStatus()))
                .map(trx -> );
    }
}
