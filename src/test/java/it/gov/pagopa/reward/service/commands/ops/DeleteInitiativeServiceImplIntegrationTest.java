package it.gov.pagopa.reward.service.commands.ops;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.test.fakers.TransactionProcessedFaker;
import it.gov.pagopa.reward.utils.AuditUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class DeleteInitiativeServiceImplIntegrationTest extends BaseIntegrationTest {
    private static final String USER_TO_DELETE = "USER_TO_DELETE";
    private final List<HpanInitiatives> hpanInitiativesTestData = new ArrayList<>();
    private final List<TransactionProcessed> transactionsProcessedTestData = new ArrayList<>();
    @Autowired
    private DroolsRuleRepository droolsRuleRepository;
    @Autowired private HpanInitiativesRepository hpanInitiativesRepository;
    @Autowired private TransactionProcessedRepository transactionProcessedRepository;
    @Autowired private UserInitiativeCountersRepository userInitiativeCountersRepository;
    @Autowired private RewardContextHolderService rewardContextHolderService;
    @Autowired private AuditUtilities auditUtilities;
    private DeleteInitiativeService service;

    @BeforeEach
    void init() {
        service = new DeleteInitiativeServiceImpl(
                droolsRuleRepository,
                hpanInitiativesRepository,
                transactionProcessedRepository,
                userInitiativeCountersRepository,
                rewardContextHolderService,
                auditUtilities);
        storeTestData();
    }
    @AfterEach
    void clearData() {
        hpanInitiativesRepository.deleteAll(hpanInitiativesTestData).block();
        transactionProcessedRepository.deleteAll(transactionsProcessedTestData).block();
    }

    private void storeTestData() {
        // hpans
        HpanInitiatives hpanInitiativesNull = HpanInitiativesFaker.mockInstance(1);
        hpanInitiativesNull.setOnboardedInitiatives(null);

        HpanInitiatives hpanInitiativesEmpty = HpanInitiativesFaker.mockInstance(2);
        hpanInitiativesEmpty.setUserId(USER_TO_DELETE);
        hpanInitiativesEmpty.setOnboardedInitiatives(new ArrayList<>());

        HpanInitiatives hpanInitiativesWithInitiatives = HpanInitiativesFaker.mockInstance(3);

        hpanInitiativesTestData.addAll(
                Objects.requireNonNull(hpanInitiativesRepository.saveAll(List.of(hpanInitiativesNull, hpanInitiativesEmpty, hpanInitiativesWithInitiatives)).collectList().block())
        );


        // transaction processed
        TransactionProcessed trxProcessedNull = TransactionProcessedFaker.mockInstanceBuilder(1)
                .initiatives(null)
                .build();

        TransactionProcessed trxProcessedEmpty = TransactionProcessedFaker.mockInstanceBuilder(2)
                .userId(USER_TO_DELETE)
                .initiatives(new ArrayList<>())
                .build();

        TransactionProcessed trxProcessedwithInitiatives = TransactionProcessedFaker.mockInstanceBuilder(3)
                .initiatives(List.of("INITIATIVE"))
                .build();

        transactionsProcessedTestData.addAll(
                Objects.requireNonNull(transactionProcessedRepository.saveAll(List.of(trxProcessedNull, trxProcessedEmpty, trxProcessedwithInitiatives))
                        .collectList().block())
        );

    }


    @Test
    void scheduleRemovedAfterInitiativeDeletionTestHandle(){
        service.removedAfterInitiativeDeletion().block();

        List<HpanInitiatives> hpanInitiativesAfter = hpanInitiativesRepository.findAll().collectList().block();

        Assertions.assertNotNull(hpanInitiativesAfter);
        Assertions.assertEquals(2, hpanInitiativesAfter.size());
        Assertions.assertTrue(
                hpanInitiativesAfter.stream()
                        .noneMatch(hpanInitiatives -> USER_TO_DELETE.equals(hpanInitiatives.getUserId()))
        );

        List<BaseTransactionProcessed> trxProcessedDbAfter = transactionProcessedRepository.findAll().collectList().block();
        Assertions.assertNotNull(trxProcessedDbAfter);
        Assertions.assertEquals(2, trxProcessedDbAfter.size());
        Assertions.assertTrue(
                trxProcessedDbAfter.stream()
                        .noneMatch(trx -> USER_TO_DELETE.equals(trx.getUserId()))
        );

    }
}