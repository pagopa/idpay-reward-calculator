package it.gov.pagopa.common.reactive.kafka.consumer;

import it.gov.pagopa.common.reactive.kafka.exception.UncommittableError;
import it.gov.pagopa.common.reactive.service.LockService;
import it.gov.pagopa.common.utils.CommonUtilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
public abstract class BaseKafkaBlockingPartitionConsumer<T, R> extends BaseKafkaConsumer<T, R> {

    private final LockService lockService;

    protected BaseKafkaBlockingPartitionConsumer(String applicationName, LockService lockService) {
        super(applicationName);
        this.lockService = lockService;
    }

    @Override
    protected Mono<R> execute(Message<String> message, Map<String, Object> ctx) {
        int lockId = ObjectUtils.firstNonNull(calculateLockId(message), -1);

        Mono<Pair<Message<String>, Integer>> acquiringLock = Mono.just(new MutablePair<>(message, lockId));
        if(lockId > -1) {
            acquiringLock = lockService.acquireLock(lockId)
                    .doOnNext(x -> log.debug("{}[LOCK_ACQUIRED] Message acquired lockId {}", getFlowName(), lockId))
                    .then(acquiringLock);
        }

        return acquiringLock
                .flatMap(m -> executeAfterLock(m, ctx));
    }

    private Mono<R> executeAfterLock(Pair<Message<String>, Integer> messageAndLockId, Map<String, Object> ctx) {
        log.trace("{} Received payload: {}", getFlowName(),  CommonUtilities.readMessagePayload(messageAndLockId.getKey()));

        final Message<String> message = messageAndLockId.getKey();

        final Consumer<? super Signal<R>> lockReleaser = x -> {
            int lockId = messageAndLockId.getValue();
            if (lockId > -1) {
                lockService.releaseLock(lockId);
                messageAndLockId.setValue(-1);
                log.debug("{}[LOCK_RELEASED] released lock having id {}", getFlowName(), lockId);
            }
        };

        ctx.put(CONTEXT_KEY_START_TIME, System.currentTimeMillis());

        return Mono.just(message)
                .mapNotNull(this::deserializeMessage)
                .flatMap(m -> execute(m, message, ctx, lockReleaser))

                .onErrorResume(UncommittableError.class, e -> {
                    log.info("Retrying after reactive pipeline error: ", e);
                    return executeAfterLock(messageAndLockId, ctx);
                })

                .doOnEach(lockReleaser);
    }

    /**
     * as default behavior, let's the base class to to release the lock. Override this method if it's possible to anticipate the release lock
     */
    @SuppressWarnings("squid:S1128") // ignore unused parameter, could be useful in subclass
    protected Mono<R> execute(T payload, Message<String> message, Map<String, Object> ctx, Consumer<? super Signal<R>> lockReleaser) {
        return execute(payload, message, ctx);
    }

    /**
     * Given a message, it will return the first not null of:
     * <ol>
     *     <li>hashcode of its MessageKey</li>
     *     <li>partitionNumber</li>
     * </ol>
     */
    protected int getMessagePartitionKey(Message<String> message) {
        String messageKey = CommonUtilities.getByteArrayHeaderValue(message, KafkaHeaders.RECEIVED_KEY);
        if (!StringUtils.isEmpty(messageKey)) {
            return messageKey.hashCode();
        } else {
            Integer partitionId = (Integer) CommonUtilities.getHeaderValue(message, KafkaHeaders.RECEIVED_PARTITION);
            return Objects.requireNonNullElseGet(partitionId, () -> message.getPayload().hashCode());
        }
    }

    /**
     * Given a message, it will calculate the lockId
     */
    public int calculateLockId(Message<String> message) {
        return Math.floorMod(Math.abs(getMessagePartitionKey(message)), lockService.getBuketSize());
    }

}
