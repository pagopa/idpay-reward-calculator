package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.function.Function;

public class MessageKeyedPreparationMapper implements Function<RewardTransactionDTO, Message<RewardTransactionDTO>> {

    @Override
    public Message<RewardTransactionDTO> apply(RewardTransactionDTO rewardTransactionDTO) {
        return MessageBuilder.withPayload(rewardTransactionDTO)
                .setHeader(KafkaHeaders.MESSAGE_KEY,rewardTransactionDTO.getUserId()).build();
    }
}
