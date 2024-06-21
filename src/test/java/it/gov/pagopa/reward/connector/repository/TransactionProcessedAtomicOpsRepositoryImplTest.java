package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.common.mongo.singleinstance.AutoConfigureSingleInstanceMongodb;
import it.gov.pagopa.common.reactive.mongo.config.ReactiveMongoConfig;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.test.fakers.TransactionProcessedFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestPropertySource(properties = {
        "de.flapdoodle.mongodb.embedded.version=4.2.24",
        "spring.data.mongodb.database=idpay",
})
@ExtendWith(SpringExtension.class)
@AutoConfigureSingleInstanceMongodb
@ContextConfiguration(classes = {TransactionProcessedAtomicOpsRepositoryImpl.class, ReactiveMongoConfig.class})
class TransactionProcessedAtomicOpsRepositoryImplTest{
    @Autowired
    private TransactionProcessedRepository transactionProcessedRepository;
    @Autowired
    private TransactionProcessedAtomicOpsRepositoryImpl transactionProcessedAtomicOpsRepositoryImpl;
    private static final String INITIATIVE_ID = "INITIATIVE_ID";

    @AfterEach
    void clearData() {
        transactionProcessedRepository
                .deleteAllById(List.of("PROVA_TRXID_1",
                        "PROVA_TRXID_2",
                        "PROVA_TRXID_3"))
                .block();
    }

    @Test
    void removeInitiativeOnTransaction() {
        String initiativeId = "INITIATIVE";
        String initiativeRemain = "ANOTHER_INITIATIVE";
        String initiativeRejected = "INITIATIVE_REJECTED";

        Reward reward = Reward.builder()
                .initiativeId(initiativeId)
                .providedRewardCents(10_00L)
                .accruedRewardCents(10_00L)
                .build();
        Reward reward2 = Reward.builder()
                .initiativeId(initiativeRemain)
                .providedRewardCents(10_00L)
                .accruedRewardCents(10_00L)
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

        transactionProcessedAtomicOpsRepositoryImpl.removeInitiativeOnTransaction(trxRewarded.getId(), initiativeId).block();
        transactionProcessedAtomicOpsRepositoryImpl.removeInitiativeOnTransaction(trxRejected.getId(), initiativeId).block();

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

    @Test
    void findByInitiativesWithBatch() {
        TransactionProcessed trxWithInitiativeList = TransactionProcessedFaker.mockInstance(3);
        trxWithInitiativeList.setInitiatives(List.of(INITIATIVE_ID));
        trxWithInitiativeList.setId("PROVA_TRXID_3");
        trxWithInitiativeList.setInitiatives(List.of(INITIATIVE_ID));

        transactionProcessedRepository.save(trxWithInitiativeList).block();

        Flux<TransactionProcessed> result = transactionProcessedAtomicOpsRepositoryImpl.findByInitiativesWithBatch(INITIATIVE_ID, 100);

        List<TransactionProcessed> transactionProcessed = result.toStream().toList();
        assertEquals(1, transactionProcessed.size());
    }

    @Test
    void findWithoutInitiativesWithBatch() {
        TransactionProcessed trxWithInitiativeWithoutList = TransactionProcessedFaker.mockInstance(3);
        trxWithInitiativeWithoutList.setInitiatives(List.of(INITIATIVE_ID));
        trxWithInitiativeWithoutList.setId("PROVA_TRXID_3");
        trxWithInitiativeWithoutList.setInitiatives(Collections.emptyList());

        transactionProcessedRepository.save(trxWithInitiativeWithoutList).block();

        Flux<TransactionProcessed> result = transactionProcessedRepository.findWithoutInitiativesWithBatch(100);

        List<TransactionProcessed> transactionProcessed = result.toStream().toList();
        assertEquals(1, transactionProcessed.size());
    }

}