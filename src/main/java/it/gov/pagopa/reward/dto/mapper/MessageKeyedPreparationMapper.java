package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class MessageKeyedPreparationMapper implements Function<RewardTransactionDTO, Message<RewardTransactionDTO>> {

    @Override
    public Message<RewardTransactionDTO> apply(RewardTransactionDTO rewardTransactionDTO) {
        return MessageBuilder.withPayload(rewardTransactionDTO)
                .setHeader(KafkaHeaders.MESSAGE_KEY,rewardTransactionDTO.getUserId()).build();
    }
}
