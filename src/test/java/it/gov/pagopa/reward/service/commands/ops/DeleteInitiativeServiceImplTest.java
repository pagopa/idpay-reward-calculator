package it.gov.pagopa.reward.service.commands.ops;

import com.mongodb.MongoException;
import com.mongodb.client.result.DeleteResult;
import it.gov.pagopa.reward.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.enums.InitiativeRewardType;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
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

import java.util.List;

@ExtendWith(MockitoExtension.class)
class DeleteInitiativeServiceImplTest {
    @Mock private DroolsRuleRepository droolsRuleRepositoryMock;
    @Mock private HpanInitiativesRepository hpanInitiativesRepositoryMock;
    @Mock private TransactionProcessedRepository transactionProcessedRepositoryMock;
    @Mock private UserInitiativeCountersRepository userInitiativeCountersRepositoryMock;
    @Mock private RewardContextHolderService rewardContextHolderServiceMock;
    @Mock private AuditUtilities auditUtilitiesMock;
    private DeleteInitiativeServiceImpl deleteInitiativeService;
    private static final String INITIATIVE_ID = "INITIATIVEID";
    private static final int PAGE_SIZE = 100;
    private static final long DELAY = 1500;
    private static final TransactionProcessed TRANSACTION_PROCESSED = TransactionProcessed.builder()
            .id("TRANSACTION_PROCESSED_ID")
            .initiatives(List.of(INITIATIVE_ID))
            .build();
    private static final HpanInitiatives HPAN_INITIATIVES = HpanInitiatives.builder()
            .hpan("HPAN")
            .onboardedInitiatives(List.of(OnboardedInitiative.builder().initiativeId(INITIATIVE_ID).build()))
            .build();

    @BeforeEach
    void setUp() {
        deleteInitiativeService = Mockito.spy(new DeleteInitiativeServiceImpl(
                droolsRuleRepositoryMock,
                hpanInitiativesRepositoryMock,
                transactionProcessedRepositoryMock,
                userInitiativeCountersRepositoryMock,
                rewardContextHolderServiceMock,
                auditUtilitiesMock,
                PAGE_SIZE,
                DELAY
        ));
    }

    @ParameterizedTest
    @EnumSource(InitiativeRewardType.class)
    void executeInitiativeOK(InitiativeRewardType initiativeRewardType) {
        String userid = "USERID";

        InitiativeConfig initiativeConfigMock = InitiativeConfig.builder()
                .initiativeId(INITIATIVE_ID)
                .initiativeRewardType(initiativeRewardType)
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(INITIATIVE_ID))
                .thenReturn(Mono.just(initiativeConfigMock));

        Mockito.when(transactionProcessedRepositoryMock.findByInitiativesWithBatch(INITIATIVE_ID, 100))
                .thenReturn(Flux.just(TRANSACTION_PROCESSED));
        if(InitiativeRewardType.DISCOUNT.equals(initiativeRewardType)) {
            Mockito.when(transactionProcessedRepositoryMock.deleteById(TRANSACTION_PROCESSED.getId()))
                    .thenReturn(Mono.empty());
        } else {
            Mockito.when(transactionProcessedRepositoryMock.removeInitiativeOnTransaction(TRANSACTION_PROCESSED.getId(), INITIATIVE_ID))
                    .thenReturn(Mono.empty());
        }

        Mockito.when(droolsRuleRepositoryMock.removeById(INITIATIVE_ID))
                .thenReturn(Mono.just(Mockito.mock(DeleteResult.class)));

        Mockito.when(hpanInitiativesRepositoryMock.findByInitiativesWithBatch(INITIATIVE_ID, 100))
                .thenReturn(Flux.just(HPAN_INITIATIVES));
        Mockito.when(hpanInitiativesRepositoryMock.removeInitiativeOnHpan(HPAN_INITIATIVES.getHpan(), INITIATIVE_ID))
                .thenReturn(Mono.empty());

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters();
        userInitiativeCounters.setId("INITIATIVE_COUNTERS_ID");
        userInitiativeCounters.setEntityId(userid);
        userInitiativeCounters.setInitiativeId(INITIATIVE_ID);
        Mockito.when(userInitiativeCountersRepositoryMock.findByInitiativesWithBatch(INITIATIVE_ID, 100))
                .thenReturn(Flux.just(userInitiativeCounters));
        Mockito.when(userInitiativeCountersRepositoryMock.deleteById(userInitiativeCounters.getId()))
                .thenReturn(Mono.empty());

        HpanInitiatives hpanWithoutInitiatives = HpanInitiativesFaker.mockInstance(1);
        Mockito.when(hpanInitiativesRepositoryMock.findWithoutInitiativesWithBatch(100))
                .thenReturn(Flux.just(hpanWithoutInitiatives));
        Mockito.when(hpanInitiativesRepositoryMock.deleteById(hpanWithoutInitiatives.getHpan()))
                .thenReturn(Mono.empty());

        TransactionProcessed trxProcessedWithoutInitiatives = TransactionProcessedFaker.mockInstance(1);
        Mockito.when(transactionProcessedRepositoryMock.findWithoutInitiativesWithBatch(100))
                .thenReturn(Flux.just(trxProcessedWithoutInitiatives));
        Mockito.when(transactionProcessedRepositoryMock.deleteById(trxProcessedWithoutInitiatives.getId()))
                .thenReturn(Mono.empty());

        String result = deleteInitiativeService.execute(INITIATIVE_ID).block();

        Assertions.assertNotNull(result);

        Mockito.verify(droolsRuleRepositoryMock, Mockito.times(1)).removeById(Mockito.anyString());
        Mockito.verify(hpanInitiativesRepositoryMock, Mockito.times(1)).removeInitiativeOnHpan(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(rewardContextHolderServiceMock, Mockito.times(1)).getInitiativeConfig(Mockito.anyString());
        Mockito.verify(transactionProcessedRepositoryMock, Mockito.times(1)).findByInitiativesWithBatch(Mockito.anyString(), Mockito.anyInt());

        if(InitiativeRewardType.DISCOUNT.equals(initiativeRewardType)) {
            Mockito.verify(transactionProcessedRepositoryMock, Mockito.times(2)).deleteById(Mockito.anyString());
            Mockito.verify(transactionProcessedRepositoryMock, Mockito.never()).removeInitiativeOnTransaction(Mockito.anyString(), Mockito.anyString());

        } else {
            Mockito.verify(transactionProcessedRepositoryMock, Mockito.times(1)).deleteById(Mockito.anyString());
            Mockito.verify(transactionProcessedRepositoryMock, Mockito.times(1)).removeInitiativeOnTransaction(Mockito.anyString(), Mockito.anyString());
        }
    }

    @Test
    void executeError() {
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(INITIATIVE_ID))
                .thenThrow(new MongoException("DUMMY_EXCEPTION"));

        try{
            deleteInitiativeService.execute(INITIATIVE_ID).block();
            Assertions.fail();
        }catch (MongoException t){
            // Do nothing
        }
    }
}