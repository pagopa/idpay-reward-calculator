package it.gov.pagopa.reward.service.reward.trx;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2TransactionProcessedMapper;
import it.gov.pagopa.reward.dto.mapper.trx.recover.RecoveredTrx2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.trx.recover.RecoveredTrx2UserInitiativeCountersMapper;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.model.counters.RewardCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.connector.repository.primary.TransactionRepository;
import it.gov.pagopa.reward.connector.repository.primary.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.service.reward.RewardNotifierService;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class RecoveryProcessedTransactionServiceTest {

    @Mock
    private UserInitiativeCountersRepository countersRepositoryMock;
    private final RecoveredTrx2RewardTransactionMapper recoveredTrx2RewardTransactionMapper = new RecoveredTrx2RewardTransactionMapper(new Transaction2RewardTransactionMapper());
    private final RecoveredTrx2UserInitiativeCountersMapper recoveredTrx2UserInitiativeCountersMapper = new RecoveredTrx2UserInitiativeCountersMapper();
    @Mock
    private RewardNotifierService rewardNotifierServiceMock;
    @Mock
    private TransactionRepository transactionRepositoryMock;
    @Mock
    private RewardContextHolderService rewardContextHolderServiceMock;

    private final Transaction2TransactionProcessedMapper transactionProcessedMapper = new Transaction2TransactionProcessedMapper();

    private RecoveryProcessedTransactionService service;

    private TransactionDTO trx;
    private TransactionProcessed trxStored;
    private Reward r1;
    private Reward r2;
    private RewardTransactionDTO expectedRewarded;

    @BeforeEach
    void init() {
        service = new RecoveryProcessedTransactionServiceImpl(
                countersRepositoryMock,
                recoveredTrx2RewardTransactionMapper,
                recoveredTrx2UserInitiativeCountersMapper,
                rewardNotifierServiceMock,
                transactionRepositoryMock, rewardContextHolderServiceMock);

        trx = TransactionDTOFaker.mockInstance(0);
        trxStored = transactionProcessedMapper.apply(RewardTransactionDTOFaker.mockInstance(0));

        r1 = new Reward("INITIATIVEID1", "ORGANIZATIONID", 1_00L);
        r1.setCounters(new RewardCounters());
        r1.getCounters().setVersion(1L);
        r2 = new Reward("INITIATIVEID2", "ORGANIZATIONID", 10_00L);
        r2.setCounters(new RewardCounters());
        r2.getCounters().setVersion(5L);

        trxStored.setRewards(Map.of(r1.getInitiativeId(), r1, r2.getInitiativeId(), r2));


        expectedRewarded = recoveredTrx2RewardTransactionMapper.apply(trx, trxStored);
    }

    @AfterEach
    void verifyNotMoreMockInvocations() {
        Mockito.verifyNoMoreInteractions(
                countersRepositoryMock,
                rewardNotifierServiceMock,
                transactionRepositoryMock
        );
    }

    private void configureTransactionRepositoryCheck(boolean expected2bePublished) {
        Mockito.when(transactionRepositoryMock.checkIfExists(trxStored.getId())).thenReturn(Mono.just(!expected2bePublished));
    }

    private void verifyNotify(boolean expected2bePublished) {
        if (expected2bePublished) {
            Mockito.verify(rewardNotifierServiceMock).notifyFallbackToErrorTopic(expectedRewarded);
        }
    }

    private List<UserInitiativeCounters> configureCountersFind(Long expectedR1Version, Long expectedR2Version) {
        List<UserInitiativeCounters> counters = Stream.of(
                expectedR1Version != null ? buildUserCounter(r1, expectedR1Version) : null,

                expectedR2Version != null ? buildUserCounter(r2, expectedR2Version) : null
        ).filter(Objects::nonNull).toList();

        Mockito.when(countersRepositoryMock.findByEntityIdAndInitiativeIdIn(trx.getUserId(), Set.of(r1.getInitiativeId(), r2.getInitiativeId())))
                .thenReturn(Flux.fromIterable(counters));

        return counters;
    }

    private UserInitiativeCounters buildUserCounter(Reward r2, Long expectedR2Version) {
        return UserInitiativeCounters.builder(trx.getUserId(), InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,r2.getInitiativeId())
                .version(expectedR2Version)
                .updateDate(trxStored.getElaborationDateTime())
                .build();
    }

//region testNoStoredRewards
    @Test
    void testNoStoredRewards_NoPublishedDifferentOffset() {
        testNoStoredRewards(true, false);
    }
    @Test
    void testNoStoredRewards_NoPublished() {
        testNoStoredRewards(false, true);
    }
    @Test
    void testNoStoredRewards_AlreadyPublished() {
        testNoStoredRewards(true, true);
    }
    void testNoStoredRewards(boolean expected2bePublished, boolean sameOffset) {
        // Given
        trxStored.setRewards(null);
        expectedRewarded.setRewards(null);

        if(!sameOffset){
            trxStored.setRuleEngineTopicOffset(-10L);
        } else {
            configureTransactionRepositoryCheck(expected2bePublished);
        }


        // When
        service.checkIf2Recover(trx, trxStored).block();

        // Then
        verifyNotify(expected2bePublished && sameOffset);
    }
//endregion

//region testCountersAlreadyUpdated
    @Test
    void testCountersAlreadyUpdated_NoPublished() {
        testCountersAlreadyUpdated(false, true);
    }
    @Test
    void testCountersAlreadyUpdated_AlreadyPublished() {
        testCountersAlreadyUpdated(true, true);
    }
    @Test
    void testCountersAlreadyUpdated_AlreadyPublishedDifferentOffset() {
        testCountersAlreadyUpdated(true, false);
    }
    private void testCountersAlreadyUpdated(boolean expected2bePublished, boolean sameOffset) {
        // Given
        configureCountersFind(r1.getCounters().getVersion(), r2.getCounters().getVersion() + 1);

        if(!sameOffset){
            trxStored.setRuleEngineTopicOffset(-10L);
        } else {
            configureTransactionRepositoryCheck(expected2bePublished);
        }

        // When
        service.checkIf2Recover(trx, trxStored).block();

        // Then
        verifyNotify(expected2bePublished && sameOffset);
    }
//endregion

    @Test
    void testRecovered() {
        // Given
        List<UserInitiativeCounters> storedCounters = configureCountersFind(null, r2.getCounters().getVersion() - 1);

        Set<UserInitiativeCounters> expectedCountersUpdated = new HashSet<>();
        expectedCountersUpdated.add(buildUserCounter(r1, r1.getCounters().getVersion()));

        expectedCountersUpdated.addAll(storedCounters.stream()
                .map(c -> (UserInitiativeCounters) c.toBuilder()
                        .version(r2.getCounters().getVersion())
                        .build())
                .toList());

        Mockito.when(countersRepositoryMock.saveAll(Mockito.<Iterable<UserInitiativeCounters>>argThat(i->new HashSet<>((List<UserInitiativeCounters>)i).equals(expectedCountersUpdated))))
                .thenReturn(Flux.fromIterable(expectedCountersUpdated));
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(Mockito.anyString())).thenAnswer(arg -> Mono.just(InitiativeConfig.builder()
                .initiativeId(arg.getArgument(0))
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                .build()));

        // When
        service.checkIf2Recover(trx, trxStored).block();

        // Then
        verifyNotify(true);
    }
}
