package it.gov.pagopa.reward.service.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import it.gov.pagopa.reward.dto.commands.CommandOperationDTO;
import it.gov.pagopa.reward.service.RewardErrorNotifierService;
import it.gov.pagopa.reward.service.commands.ops.DeleteInitiativeService;
import it.gov.pagopa.reward.utils.CommandsConstants;
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
public class CommandsMediatorServiceImpl extends BaseKafkaConsumer<CommandOperationDTO, String> implements CommandsMediatorService {

    private final Duration commitDelay;
    private final DeleteInitiativeService deleteInitiativeService;
    private final RewardErrorNotifierService rewardErrorNotifierService;
    private final ObjectReader objectReader;

    protected CommandsMediatorServiceImpl(
            @Value("${spring.application.name}") String applicationName,
            @Value("${spring.cloud.stream.kafka.bindings.commandsConsumer-in-0.consumer.ackTime}")  long commitMillis,
            DeleteInitiativeService deleteInitiativeService, RewardErrorNotifierService rewardErrorNotifierService,
            ObjectMapper objectMapper) {
        super(applicationName);
        this.commitDelay = Duration.ofMillis(commitMillis);
        this.deleteInitiativeService = deleteInitiativeService;
        this.rewardErrorNotifierService = rewardErrorNotifierService;
        this.objectReader = objectMapper.readerFor(CommandOperationDTO.class);
    }

    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<String>> afterCommits2subscribe) {
        afterCommits2subscribe
                .subscribe(r -> log.info("[REWARD_CALCULATOR_COMMANDS] Processed offsets committed successfully"));
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> rewardErrorNotifierService.notifyRewardCommands(message, "[REWARD_CALCULATOR_COMMANDS] Unexpected JSON", false, e);
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {
        rewardErrorNotifierService.notifyRewardCommands(message, "[REWARD_CALCULATOR_COMMANDS] An error occurred evaluating commands", true, e);
    }

    @Override
    protected Mono<String> execute(CommandOperationDTO payload, Message<String> message, Map<String, Object> ctx) {
        if(CommandsConstants.COMMANDS_OPERATION_TYPE_DELETE_INITIATIVE.equals(payload.getOperationType())){
            return deleteInitiativeService.execute(payload.getEntityId());
        }
        log.debug("[REWARD_NOTIFICATION_COMMANDS] Not handled operation type {}", payload.getOperationType());
        return Mono.empty();
    }

    @Override
    public String getFlowName() {
        return "REWARD_CALCULATOR_COMMANDS";
    }

}
