package it.gov.pagopa.reward.connector.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.test.fakers.TransactionProcessedFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TransactionProcessedAtomicOpsRepositoryImplTest extends BaseIntegrationTest {

    @Autowired
    private TransactionProcessedRepository transactionProcessedRepository;
    @Autowired
    private TransactionProcessedAtomicOpsRepositoryImpl transactionProcessedAtomicOpsRepositoryImpl;

    @AfterEach
    void clearData() {
        transactionProcessedRepository.deleteAllById(List.of("TRX1", "TRX2")).block();
    }

    @Test
    void deleteByInitiativeId() {
        String initiativeId = "INITIATIVE";

        Reward reward = Reward.builder()
                .initiativeId(initiativeId)
                .providedReward(BigDecimal.TEN)
                .accruedReward(BigDecimal.TEN)
                .build();
        TransactionProcessed trxRewarded = TransactionProcessedFaker.mockInstance(1);
        trxRewarded.setId("TRX1");
        trxRewarded.setRewards(Map.of(initiativeId, reward));
        trxRewarded.setInitiatives(List.of(initiativeId));
        trxRewarded.setInitiativeRejectionReasons(null);
        trxRewarded.setStatus(RewardConstants.REWARD_STATE_REWARDED);

        TransactionProcessed trxRejected = TransactionProcessedFaker.mockInstance(2);
        trxRejected.setId("TRX2");
        trxRejected.setRewards(null);
        trxRejected.setInitiatives(List.of(initiativeId));
        trxRejected.setInitiativeRejectionReasons(Map.of(initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_DUPLICATE_CORRELATION_ID)));
        trxRejected.setStatus(RewardConstants.REWARD_STATE_REJECTED);

        transactionProcessedRepository.saveAll(List.of(trxRewarded,trxRejected)).blockLast();

        List<TransactionProcessed> result = transactionProcessedAtomicOpsRepositoryImpl.deleteByInitiativeId(initiativeId).collectList().block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(0, transactionProcessedRepository.findAll().count().block());
    }

    @Test
    void findAndRemoveInitiativeOnTransaction() {
        String initiativeId = "INITIATIVE";
        String initiativeRemain = "ANOTHER_INITIATIVE";

        Reward reward = Reward.builder()
                .initiativeId(initiativeId)
                .providedReward(BigDecimal.TEN)
                .accruedReward(BigDecimal.TEN)
                .build();
        Reward reward2 = Reward.builder()
                .initiativeId(initiativeRemain)
                .providedReward(BigDecimal.TEN)
                .accruedReward(BigDecimal.TEN)
                .build();
        TransactionProcessed trxRewarded = TransactionProcessedFaker.mockInstance(1);
        trxRewarded.setId("TRX1");
        trxRewarded.setRewards(Map.of(initiativeId, reward,
                initiativeRemain, reward2));
        trxRewarded.setInitiatives(List.of(initiativeId));
        trxRewarded.setInitiativeRejectionReasons(Map.of("INITIATIVE_REJECTED", List.of(RewardConstants.TRX_REJECTION_REASON_DUPLICATE_CORRELATION_ID)));
        trxRewarded.setStatus(RewardConstants.REWARD_STATE_REWARDED);


        TransactionProcessed trxRejected = TransactionProcessedFaker.mockInstance(2);
        trxRejected.setId("TRX2");
        trxRejected.setRewards(null);
        trxRejected.setInitiatives(List.of(initiativeId));
        trxRejected.setRewards(new HashMap<>());
        trxRejected.setInitiativeRejectionReasons(Map.of(initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_DUPLICATE_CORRELATION_ID)));
        trxRejected.setStatus(RewardConstants.REWARD_STATE_REJECTED);


        transactionProcessedRepository.saveAll(List.of(trxRewarded, trxRejected)).count().block();

        UpdateResult result = transactionProcessedAtomicOpsRepositoryImpl.findAndRemoveInitiativeOnTransaction(initiativeId).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.getModifiedCount());

        transactionProcessedRepository.findAll()
                .toStream()
                .forEach(trx ->
                    Assertions.assertFalse(trx.getInitiatives().contains(initiativeId), "Not remove initiative from list")
                );
    }
}