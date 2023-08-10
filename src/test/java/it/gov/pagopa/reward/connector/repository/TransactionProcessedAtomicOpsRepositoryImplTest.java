package it.gov.pagopa.reward.connector.repository;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.test.fakers.TransactionProcessedFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
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
        transactionProcessedRepository
                .deleteAllById(List.of("PROVA_TRXID_1",
                        "PROVA_TRXID_2",
                        "PROVA_TRXID_3"))
                .block();
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
        trxRewarded.setId("PROVA_TRXID_1");
        trxRewarded.setRewards(Map.of(initiativeId, reward));
        trxRewarded.setInitiatives(List.of(initiativeId));
        trxRewarded.setInitiativeRejectionReasons(null);
        trxRewarded.setStatus(RewardConstants.REWARD_STATE_REWARDED);

        TransactionProcessed trxRejected = TransactionProcessedFaker.mockInstance(2);
        trxRejected.setId("PROVA_TRXID_2");
        trxRejected.setRewards(null);
        trxRejected.setInitiatives(List.of(initiativeId));
        trxRejected.setInitiativeRejectionReasons(Map.of(initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_DUPLICATE_CORRELATION_ID)));
        trxRejected.setStatus(RewardConstants.REWARD_STATE_REJECTED);

        transactionProcessedRepository.saveAll(List.of(trxRewarded,trxRejected)).blockLast();

        DeleteResult result = transactionProcessedAtomicOpsRepositoryImpl.removeByInitiativeId(initiativeId).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.getDeletedCount());
        Assertions.assertEquals(0, transactionProcessedRepository.count().block());
    }

    @Test
    void findAndRemoveInitiativeOnTransaction() {
        String initiativeId = "INITIATIVE";
        String initiativeRemain = "ANOTHER_INITIATIVE";
        String initiativeRejected = "INITIATIVE_REJECTED";

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
        trxRewarded.setId("PROVA_TRXID_1");
        trxRewarded.setRewards(Map.of(initiativeId, reward,
                initiativeRemain, reward2));
        trxRewarded.setInitiatives(List.of(initiativeId, initiativeRemain, initiativeRejected));
        trxRewarded.setInitiativeRejectionReasons(Map.of(initiativeRejected, List.of(RewardConstants.TRX_REJECTION_REASON_DUPLICATE_CORRELATION_ID)));
        trxRewarded.setStatus(RewardConstants.REWARD_STATE_REWARDED);


        TransactionProcessed trxRejected = TransactionProcessedFaker.mockInstance(2);
        trxRejected.setId("PROVA_TRXID_2");
        trxRejected.setRewards(null);
        trxRejected.setInitiatives(List.of(initiativeId));
        trxRejected.setInitiativeRejectionReasons(Map.of(initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_DUPLICATE_CORRELATION_ID)));
        trxRejected.setStatus(RewardConstants.REWARD_STATE_REJECTED);


        transactionProcessedRepository.saveAll(List.of(trxRewarded, trxRejected)).count().block();

        UpdateResult result = transactionProcessedAtomicOpsRepositoryImpl.removeInitiativeOnTransaction(initiativeId).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.getModifiedCount());

        Assertions.assertEquals(2L, transactionProcessedRepository.count().block());

        BaseTransactionProcessed trxRewardedAfter = transactionProcessedRepository.findById("PROVA_TRXID_1").block();

        Assertions.assertNotNull(trxRewardedAfter);
        Assertions.assertEquals(List.of(initiativeRemain, initiativeRejected), trxRewardedAfter.getInitiatives());
        Assertions.assertEquals(Map.of(initiativeRemain, reward2), trxRewardedAfter.getRewards());
        Assertions.assertEquals(Map.of(initiativeRejected, List.of(RewardConstants.TRX_REJECTION_REASON_DUPLICATE_CORRELATION_ID)),
                trxRewardedAfter.getInitiativeRejectionReasons());

        BaseTransactionProcessed trxRejectedAfter = transactionProcessedRepository.findById("PROVA_TRXID_2").block();

        Assertions.assertNotNull(trxRejectedAfter);
        Assertions.assertEquals(new ArrayList<>(), trxRejectedAfter.getInitiatives());
        Assertions.assertNull(trxRejectedAfter.getRewards());
        Assertions.assertEquals(new HashMap<>(), trxRejectedAfter.getInitiativeRejectionReasons());
    }

    @Test
    void deleteTransactionsWithoutInitiative(){
        TransactionProcessed trxNullList = TransactionProcessedFaker.mockInstance(1);
        trxNullList.setId("PROVA_TRXID_1");
        trxNullList.setInitiatives(null);

        TransactionProcessed trxEmptyList = TransactionProcessedFaker.mockInstance(2);
        trxEmptyList.setId("PROVA_TRXID_2");
        trxEmptyList.setInitiatives(new ArrayList<>());

        TransactionProcessed trxWithInitiativeList = TransactionProcessedFaker.mockInstance(3);
        trxWithInitiativeList.setId("PROVA_TRXID_3");
        trxWithInitiativeList.setInitiatives(List.of("INITIATIVE"));


        transactionProcessedRepository.saveAll(List.of(trxNullList, trxEmptyList, trxWithInitiativeList)).blockLast();

        List<TransactionProcessed> result = transactionProcessedAtomicOpsRepositoryImpl.deleteTransactionsWithoutInitiative().collectList().block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(List.of(trxEmptyList), result);


        BaseTransactionProcessed trxNullInitiativesAfter = transactionProcessedRepository.findById("PROVA_TRXID_1").block();
        Assertions.assertNotNull(trxNullInitiativesAfter);

        BaseTransactionProcessed trxEmptyInitiativesAfter = transactionProcessedRepository.findById("PROVA_TRXID_2").block();
        Assertions.assertNull(trxEmptyInitiativesAfter);

        BaseTransactionProcessed trxWithInitiativesAfter = transactionProcessedRepository.findById("PROVA_TRXID_3").block();
        Assertions.assertNotNull(trxWithInitiativesAfter);

    }
}