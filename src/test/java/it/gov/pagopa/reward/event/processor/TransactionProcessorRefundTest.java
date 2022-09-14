package it.gov.pagopa.reward.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

class TransactionProcessorRefundTest extends BaseTransactionProcessorTest {

    private final String initiativeId = "INITIATIVEID";

    @Test
    void test() throws JsonProcessingException {
        int completeRefundedTrxs = 10;
        long maxWaitingMs = 10000;

        publishRewardRules(List.of(InitiativeReward2BuildDTOFaker.mockInstanceBuilder(1, Collections.emptySet(), RewardValueDTO.class)
                        .initiativeId(initiativeId)
                .general(InitiativeGeneralDTO.builder()
                        .beneficiaryBudget(BigDecimal.valueOf(100))
                        .build())
                .rewardRule(RewardValueDTO.builder()
                        .rewardValue(BigDecimal.TEN)
                        .build())
                .build()
        ));

        List<TransactionDTO> trxs = new ArrayList<>(IntStream.range(0, completeRefundedTrxs).mapToObj(this::buildTotalRefundRequests).flatMap(List::stream).toList());

        trxs.forEach(t->onboardHpan(t.getHpan(), t.getTrxDate().toLocalDateTime(), null, initiativeId));

        long timePublishOnboardingStart = System.currentTimeMillis();
        trxs.forEach(i -> publishIntoEmbeddedKafka(topicRewardProcessorRequest, null, i.getUserId(), i));
        long timePublishingOnboardingRequest = System.currentTimeMillis() - timePublishOnboardingStart;

        long timeConsumerResponse = System.currentTimeMillis();
        List<ConsumerRecord<String, String>> payloadConsumed = consumeMessages(topicRewardProcessorOutcome, completeRefundedTrxs, maxWaitingMs);
        long timeEnd = System.currentTimeMillis();

        long timeConsumerResponseEnd = timeEnd - timeConsumerResponse;
        Assertions.assertEquals(trxs.size(), payloadConsumed.size());

        for (ConsumerRecord<String, String> p : payloadConsumed) {
            checkResponse(objectMapper.readValue(p.value(), RewardTransactionDTO.class), completeRefundedTrxs);
        }

        System.out.printf("""
                        ************************
                        Time spent to send %d trx messages: %d millis
                        Time spent to consume reward responses: %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                trxs.size(),
                timePublishingOnboardingRequest,
                timeConsumerResponseEnd,
                timeEnd - timePublishOnboardingStart
        );
    }

    private List<TransactionDTO> buildTotalRefundRequests(int bias) {
        return totalRefundUseCases.get(bias % totalRefundUseCases.size()).getLeft().apply(bias);
    }

    private void checkResponse(RewardTransactionDTO rewardedTrx, int completeRefundedTrxs) {
        String hpan = rewardedTrx.getHpan();
        int biasRetrieve = Integer.parseInt(hpan.substring(4));

        if (rewardedTrx.getOperationType().equals("00")) {
            checkChargeOp(rewardedTrx, biasRetrieve < completeRefundedTrxs, biasRetrieve % completeRefundedTrxs);
        } else {
            if (biasRetrieve < completeRefundedTrxs) {
                checkTotalRefundOp(rewardedTrx, biasRetrieve);
            }
        }
    }

    private void checkChargeOp(RewardTransactionDTO rewardedTrx, boolean totalRefundUseCase, int useCase) {
        Assertions.assertEquals(OperationType.CHARGE, rewardedTrx.getOperationTypeTranscoded());
        Assertions.assertEquals("REWARDED", rewardedTrx.getStatus());
        if(totalRefundUseCase){
            totalRefundUseCases.get(useCase).getMiddle().accept(rewardedTrx);
        }
    }

    private void checkTotalRefundOp(RewardTransactionDTO rewardedTrx, int useCase) {
        Assertions.assertEquals(OperationType.REFUND, rewardedTrx.getOperationTypeTranscoded());
        Assertions.assertEquals("REWARDED", rewardedTrx.getStatus());
        totalRefundUseCases.get(useCase).getRight().accept(rewardedTrx);
    }

    //region total refund useCases
    private final List<Triple<Function<Integer, List<TransactionDTO>>, Consumer<RewardTransactionDTO>, Consumer<RewardTransactionDTO>>> totalRefundUseCases = List.of(
            // Base use case
            Triple.of(
                    i -> {
                        final TransactionDTO trx = TransactionDTOFaker.mockInstance(i);
                        trx.setAmount(BigDecimal.TEN);
                        final TransactionDTO totalRefund = TransactionDTOFaker.mockInstance(i);
                        totalRefund.setOperationType("01");
                        totalRefund.setTrxDate(trx.getTrxDate().plusDays(i + 1));
                        totalRefund.setAmount(trx.getAmount());
                        return List.of(trx, totalRefund);
                    },
                    chargeReward -> assertRewardedState(chargeReward, initiativeId, BigDecimal.ONE, false),
                    refundReward -> assertRewardedState(refundReward, initiativeId, BigDecimal.valueOf(-1), false)
            )
    );
    //endregion
}
