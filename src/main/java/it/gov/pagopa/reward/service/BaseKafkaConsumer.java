package it.gov.pagopa.reward.service;

import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Base class to extend in order to configure a timed commit behavior when using KafkaBinder.
 * Other than extend this class, you should:
 * <ol>
 *     <li>Turn off the autoCommit (spring.cloud.stream.kafka.bindings.BINDINGNAME.consumer.autoCommitOffset=false)</li>
 *     <li>Set the ackMode to MANUAL_IMMEDIATE (spring.cloud.stream.kafka.bindings.BINDINGNAME.consumer.ackMode=MANUAL_IMMEDIATE)</li>
 * </ol>
 * @param <T> The type of the message to read and deserialize
 * @param <R> The type of the message resulted
 */
public abstract class BaseKafkaConsumer<T, R> {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KafkaAcknowledgeResult<T> {
        private Acknowledgment ack;
        private T result;
    }

    /** It will ask the superclass to handle the messages, then sequentially it will acknowledge them */
    public final void execute(Flux<Message<String>> initiativeBeneficiaryRuleDTOFlux) {
        Flux<List<R>> processUntilCommits =
                initiativeBeneficiaryRuleDTOFlux
                        .flatMapSequential(this::executeAcknowledgeAware)

                        .buffer(getCommitDelay())
                        .map(p -> p.stream()
                                .map(ack2entity -> {
                                    if (ack2entity.getAck() != null) {
                                        ack2entity.getAck().acknowledge();
                                    }
                                    return ack2entity.getResult();
                                })
                                .filter(Objects::nonNull)
                                .toList()
                        );

        subscribeAfterCommits(processUntilCommits);
    }

    /** The {@link Duration} to wait before to commit processed messages */
    protected abstract Duration getCommitDelay();

    /** {@link Flux} to which subscribe in order to start its execution and eventually perform some logic on results */
    protected abstract void subscribeAfterCommits(Flux<List<R>> afterCommits2subscribe);

    private Mono<KafkaAcknowledgeResult<R>> executeAcknowledgeAware(Message<String> message) {
        Acknowledgment ack = message.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);
        KafkaAcknowledgeResult<R> defaultAck = new KafkaAcknowledgeResult<>(ack, null);

        return execute(message)
                .map(r -> new KafkaAcknowledgeResult<>(ack, r))
                .defaultIfEmpty(defaultAck)

                .onErrorResume(e -> {
                    notifyError(message, e);
                    return Mono.just(defaultAck);
                });
    }

    /** It will deserialize the message and then call the {@link #execute(Object, Message)} method */
    protected Mono<R> execute(Message<String> message){
        return Mono.just(message)
                .mapNotNull(this::deserializeMessage)
                .flatMap(payload->execute(payload, message));
    }

    /** The {@link ObjectReader} to use in order to deserialize the input message */
    protected abstract ObjectReader getObjectReader();
    /** The action to take if the deserialization will throw an error */
    protected abstract Consumer<Throwable> onDeserializationError(Message<String> message);
    /** The action to take if an unexpected exception occurs */
    protected abstract void notifyError(Message<String> message, Throwable e);

    /** The function invoked in order to process the current message */
    protected abstract Mono<R> execute(T payload, Message<String> message);

    /** It will read and deserialize {@link Message#getPayload()} using the given {@link #getObjectReader()} */
    protected T deserializeMessage(Message<String> message) {
        return Utils.deserializeMessage(message, getObjectReader(), onDeserializationError(message));
    }

}
