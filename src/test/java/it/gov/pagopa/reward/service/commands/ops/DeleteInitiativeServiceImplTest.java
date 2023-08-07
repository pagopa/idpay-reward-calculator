package it.gov.pagopa.reward.service.commands.ops;

import com.mongodb.MongoException;
import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.enums.InitiativeRewardType;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.test.fakers.TransactionProcessedFaker;
import it.gov.pagopa.reward.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@ExtendWith(MockitoExtension.class)
class DeleteInitiativeServiceImplTest {
    @Mock private DroolsRuleRepository droolsRuleRepositoryMock;
    @Mock private HpanInitiativesRepository hpanInitiativesRepositoryMock;
    @Mock private TransactionProcessedRepository transactionProcessedRepositoryMock;
    @Mock private UserInitiativeCountersRepository userInitiativeCountersRepositoryMock;
    @Mock private RewardContextHolderService rewardContextHolderServiceMock;
    @Mock private AuditUtilities auditUtilitiesMock;
    private DeleteInitiativeServiceImpl deleteInitiativeService;

    @BeforeEach
    void setUp() {
        deleteInitiativeService = Mockito.spy(new DeleteInitiativeServiceImpl(
                droolsRuleRepositoryMock,
                hpanInitiativesRepositoryMock,
                transactionProcessedRepositoryMock,
                userInitiativeCountersRepositoryMock,
                rewardContextHolderServiceMock,
                auditUtilitiesMock
        ));
    }

    @ParameterizedTest
    @EnumSource(InitiativeRewardType.class)
    void executeInitiativeOK(InitiativeRewardType initiativeRewardType) {
        String initiativeId = "INITIATIVEID";
        String userid = "USERID";

        InitiativeConfig initiativeConfigMock = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .initiativeRewardType(initiativeRewardType)
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId))
                .thenReturn(Mono.just(initiativeConfigMock));

        TransactionProcessed trxMock = TransactionProcessedFaker.mockInstance(1);
        if(InitiativeRewardType.DISCOUNT.equals(initiativeRewardType)) {
            Mockito.when(transactionProcessedRepositoryMock.deleteByInitiativeId(initiativeId))
                    .thenReturn(Flux.just(trxMock));
        } else {
            Mockito.when(transactionProcessedRepositoryMock.findAndRemoveInitiativeOnTransaction(initiativeId))
                    .thenReturn(Mono.just(Mockito.mock(UpdateResult.class)));
        }

        Mockito.when(droolsRuleRepositoryMock.deleteById(initiativeId))
                .thenReturn(Mono.just(Mockito.mock(Void.class)));

        UpdateResult updateResultMock = Mockito.mock(UpdateResult.class);
        Mockito.when(hpanInitiativesRepositoryMock.findAndRemoveInitiativeOnHpan(initiativeId))
                .thenReturn(Mono.just(updateResultMock));

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters();
        userInitiativeCounters.setUserId(userid);
        userInitiativeCounters.setInitiativeId(initiativeId);
        Mockito.when(userInitiativeCountersRepositoryMock.deleteByInitiativeId(initiativeId))
                .thenReturn(Flux.just(userInitiativeCounters));

        String result = deleteInitiativeService.execute(initiativeId).block();

        Assertions.assertNotNull(result);

        Mockito.verify(droolsRuleRepositoryMock, Mockito.times(1)).deleteById(Mockito.anyString());
        Mockito.verify(hpanInitiativesRepositoryMock, Mockito.times(1)).findAndRemoveInitiativeOnHpan(Mockito.anyString());
        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.times(1)).deleteByInitiativeId(Mockito.anyString());
        Mockito.verify(rewardContextHolderServiceMock, Mockito.times(1)).getInitiativeConfig(Mockito.anyString());

        if(InitiativeRewardType.DISCOUNT.equals(initiativeRewardType)) {
            Mockito.verify(transactionProcessedRepositoryMock, Mockito.times(1)).deleteByInitiativeId(Mockito.anyString());
            Mockito.verify(transactionProcessedRepositoryMock, Mockito.never()).findAndRemoveInitiativeOnTransaction(Mockito.anyString());

        } else {
            Mockito.verify(transactionProcessedRepositoryMock, Mockito.never()).deleteByInitiativeId(Mockito.anyString());
            Mockito.verify(transactionProcessedRepositoryMock, Mockito.times(1)).findAndRemoveInitiativeOnTransaction(Mockito.anyString());
        }
    }

    @Test
    void executeError() {
        String initiativeId = "INITIATIVEID";
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId))
                .thenThrow(new MongoException("DUMMY_EXCEPTION"));

        try{
            deleteInitiativeService.execute(initiativeId).block();
            Assertions.fail();
        }catch (Throwable t){
            Assertions.assertTrue(t instanceof  MongoException);
        }
    }
    @Test
    void testSchedule(){
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithoutInitiative(1);
        hpanInitiatives.setOnboardedInitiatives(new ArrayList<>());
        Mockito.when(hpanInitiativesRepositoryMock.deleteHpanWithoutInitiative())
                .thenReturn(Flux.just(hpanInitiatives));

        TransactionProcessed trxProcessed = TransactionProcessedFaker.mockInstance(1);
        trxProcessed.setInitiatives(new ArrayList<>());
        Mockito.when(transactionProcessedRepositoryMock.deleteTransactionsWithoutInitiative())
                .thenReturn(Flux.just());

        deleteInitiativeService.scheduleRemovedAfterInitiativeDeletion();

        Mockito.verify(hpanInitiativesRepositoryMock, Mockito.times(1)).deleteHpanWithoutInitiative();
        Mockito.verify(transactionProcessedRepositoryMock, Mockito.times(1)).deleteTransactionsWithoutInitiative();
    }
}