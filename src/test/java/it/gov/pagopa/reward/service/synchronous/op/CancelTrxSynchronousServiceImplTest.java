package it.gov.pagopa.reward.service.synchronous.op;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.RewardTransaction2SynchronousTransactionResponseDTOMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionAuthRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.InitiativeRewardType;
import it.gov.pagopa.reward.exception.custom.InitiativeNotActiveException;
import it.gov.pagopa.reward.exception.custom.PendingCounterException;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.service.reward.OnboardedInitiativesService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateService;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionAuthRequestDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.INITIATIVE_NOT_ACTIVE_FOR_USER;
import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.PENDING_COUNTER;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class CancelTrxSynchronousServiceImplTest{

    private static final String INITIATIVEID = "INITIATIVEID";
    private static final String ORGANIZATIONID = "ORGANIZATIONID";
    @Mock
    private UserInitiativeCountersRepository userInitiativeCountersRepositoryMock;
    @Mock
    private RewardContextHolderService rewardContextHolderServiceMock;
    @Mock
    private UserInitiativeCountersUpdateService userInitiativeCountersUpdateServiceMock;
    @Mock
    private OnboardedInitiativesService onboardedInitiativesServiceMock;

    private CancelTrxSynchronousServiceImpl cancelTrxSynchronousService;

    @BeforeEach
    void setUp() {
        cancelTrxSynchronousService =
                new CancelTrxSynchronousServiceImpl("01",
                        userInitiativeCountersRepositoryMock,
                        new RewardTransaction2SynchronousTransactionResponseDTOMapper(),
                        rewardContextHolderServiceMock,
                        new Transaction2RewardTransactionMapper(),
                        userInitiativeCountersUpdateServiceMock,
                        onboardedInitiativesServiceMock,
                        new SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper("00"));
    }

    @Test
    void cancelTrxSuccess(){
        // Given
        SynchronousTransactionAuthRequestDTO trxCancelRequest = SynchronousTransactionAuthRequestDTOFaker.mockInstance(1);

        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(INITIATIVEID))
                .thenReturn(Mono.just(getInitiativeConfig()));

        Mockito.when(rewardContextHolderServiceMock.getRewardRulesKieInitiativeIds()).thenReturn(Set.of(INITIATIVEID));

        OnboardedInitiative onboardingInitiative = OnboardedInitiative.builder().initiativeId(INITIATIVEID).familyId("FAMILYID").build();
        Mockito.when(onboardedInitiativesServiceMock.isOnboarded(any(),any(),eq(INITIATIVEID))).thenReturn(Mono.just(Pair.of(getInitiativeConfig(), onboardingInitiative)));

        Mockito.when(userInitiativeCountersRepositoryMock.findById(UserInitiativeCounters.buildId(trxCancelRequest.getUserId(), INITIATIVEID)))
                .thenReturn(Mono.just(getUserInitiativeCounters(trxCancelRequest)));

        RewardTransactionDTO rewardTrx = RewardTransactionDTOFaker.mockInstance(1);
        rewardTrx.setInitiatives(List.of(INITIATIVEID));
        rewardTrx.setUserId(trxCancelRequest.getUserId());
        rewardTrx.setRewards(Map.of(INITIATIVEID, new Reward(INITIATIVEID, ORGANIZATIONID, CommonUtilities.centsToEuro(trxCancelRequest.getRewardCents()))));

        Mockito.when(userInitiativeCountersUpdateServiceMock.update(any(), any()))
                .thenReturn(Mono.just(rewardTrx));

        Mockito.when(userInitiativeCountersRepositoryMock.save(any())).thenReturn(Mono.just(getUserInitiativeCounters(trxCancelRequest)));

        //When
        SynchronousTransactionResponseDTO result = cancelTrxSynchronousService.cancelTransaction(trxCancelRequest, INITIATIVEID).block();

        //Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result, "rejectionReasons");

        Mockito.verify(rewardContextHolderServiceMock, Mockito.times(2)).getInitiativeConfig(any());
        Mockito.verify(rewardContextHolderServiceMock, Mockito.times(1)).getRewardRulesKieInitiativeIds();
        Mockito.verify(onboardedInitiativesServiceMock, Mockito.times(1)).isOnboarded(any(),any(),any());
        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.times(1)).findById(anyString());
        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.times(1)).save(any());
        Mockito.verify(userInitiativeCountersUpdateServiceMock, Mockito.times(1)).update(any(),any());
    }

    @Test
    void cancelTrx_userNotOnboardedError() {
        //Given
        SynchronousTransactionAuthRequestDTO trxCancelRequest = SynchronousTransactionAuthRequestDTOFaker.mockInstance(1);

        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(INITIATIVEID))
                .thenReturn(Mono.just(getInitiativeConfig()));

        Mockito.when(rewardContextHolderServiceMock.getRewardRulesKieInitiativeIds()).thenReturn(Set.of(INITIATIVEID));

        OnboardedInitiative onboardingInitiative = OnboardedInitiative.builder().initiativeId(INITIATIVEID).familyId("FAMILYID").build();
        Mockito.when(onboardedInitiativesServiceMock.isOnboarded(any(),any(),eq(INITIATIVEID))).thenReturn(Mono.just(Pair.of(getInitiativeConfig(), onboardingInitiative)));

        Mockito.when(userInitiativeCountersRepositoryMock.findById(UserInitiativeCounters.buildId(trxCancelRequest.getUserId(), INITIATIVEID)))
                .thenReturn(Mono.empty());

        //When
        Mono<SynchronousTransactionResponseDTO> monoResult = cancelTrxSynchronousService.cancelTransaction(trxCancelRequest, INITIATIVEID);
        InitiativeNotActiveException exceptionResult = Assertions.assertThrows(InitiativeNotActiveException.class, monoResult::block);

        //Then
        Assertions.assertNotNull(exceptionResult);
        Assertions.assertEquals(INITIATIVE_NOT_ACTIVE_FOR_USER, exceptionResult.getCode());
        Assertions.assertEquals(String.format(RewardConstants.ExceptionMessage.INITIATIVE_NOT_ACTIVE_FOR_USER_MSG,INITIATIVEID), exceptionResult.getMessage());
        Assertions.assertNotNull(exceptionResult.getPayload());

        Mockito.verify(rewardContextHolderServiceMock, Mockito.times(1)).getInitiativeConfig(any());
        Mockito.verify(rewardContextHolderServiceMock, Mockito.times(1)).getRewardRulesKieInitiativeIds();
        Mockito.verify(onboardedInitiativesServiceMock, Mockito.times(1)).isOnboarded(any(),any(),any());
        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.times(1)).findById(anyString());
        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.never()).save(any());
        Mockito.verify(userInitiativeCountersUpdateServiceMock, Mockito.never()).update(any(),any());
    }

    @Test
    void cancelTrx_pendingTrxError(){
        //Given
        SynchronousTransactionAuthRequestDTO trxCancelRequest = SynchronousTransactionAuthRequestDTOFaker.mockInstance(1);

        InitiativeConfig initiativeConfig = getInitiativeConfig();
        initiativeConfig.setBeneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF);
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(INITIATIVEID))
                .thenReturn(Mono.just(initiativeConfig));

        Mockito.when(rewardContextHolderServiceMock.getRewardRulesKieInitiativeIds()).thenReturn(Set.of(INITIATIVEID));

        OnboardedInitiative onboardingInitiative = OnboardedInitiative.builder().initiativeId(INITIATIVEID).familyId("FAMILYID").build();
        Mockito.when(onboardedInitiativesServiceMock.isOnboarded(any(),any(),eq(INITIATIVEID))).thenReturn(Mono.just(Pair.of(initiativeConfig, onboardingInitiative)));

        UserInitiativeCounters userCounter = getUserInitiativeCounters(trxCancelRequest);
        userCounter.setPendingTrx(TransactionDTOFaker.mockInstance(2));

        Mockito.when(userInitiativeCountersRepositoryMock.findById(UserInitiativeCounters.buildId("FAMILYID", INITIATIVEID)))
                .thenReturn(Mono.just(userCounter));


        //When
        Mono<SynchronousTransactionResponseDTO> monoResult = cancelTrxSynchronousService.cancelTransaction(trxCancelRequest, INITIATIVEID);
        PendingCounterException exceptionResult = Assertions.assertThrows(PendingCounterException.class, monoResult::block);

        //Then
        Assertions.assertNotNull(exceptionResult);
        Assertions.assertEquals(PENDING_COUNTER, exceptionResult.getCode());
        Assertions.assertEquals(RewardConstants.ExceptionMessage.PENDING_COUNTER, exceptionResult.getMessage());

        Mockito.verify(rewardContextHolderServiceMock, Mockito.times(1)).getInitiativeConfig(any());
        Mockito.verify(rewardContextHolderServiceMock, Mockito.times(1)).getRewardRulesKieInitiativeIds();
        Mockito.verify(onboardedInitiativesServiceMock, Mockito.times(1)).isOnboarded(any(),any(),any());
        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.times(1)).findById(anyString());
        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.never()).save(any());
        Mockito.verify(userInitiativeCountersUpdateServiceMock, Mockito.never()).update(any(),any());
    }


    @Test
    void trx2processedTest() {
        TransactionDTO trxDto = TransactionDTOFaker.mockInstance(1);
        trxDto.setTrxChargeDate(trxDto.getTrxDate());
        trxDto.setEffectiveAmount(BigDecimal.TEN);
        trxDto.setAmountCents(10_00L);

        TransactionProcessed resultTrxProcessed = cancelTrxSynchronousService.trx2processed(trxDto, "INITIATIVEID", "ORGANIZATIONID", BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_DOWN));

        Assertions.assertNotNull(resultTrxProcessed);
        TestUtils.checkNotNullFields(resultTrxProcessed,
                "refundInfo",
                "elaborationDateTime");

    }

    private static InitiativeConfig getInitiativeConfig() {
        return InitiativeConfig.builder()
                .initiativeId(INITIATIVEID)
                .organizationId(ORGANIZATIONID)
                .initiativeName("INITIATIVENAME")
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                .startDate(LocalDate.now().minusMonths(10L))
                .endDate(LocalDate.now().plusYears(2))
                .initiativeRewardType(InitiativeRewardType.DISCOUNT)
                .dailyThreshold(false)
                .weeklyThreshold(false)
                .monthlyThreshold(false)
                .yearlyThreshold(false)
                .beneficiaryBudget(BigDecimal.valueOf(300L))
                .build();
    }

    private static UserInitiativeCounters getUserInitiativeCounters(SynchronousTransactionAuthRequestDTO trxCancelRequest) {
        return UserInitiativeCounters
                .builder(trxCancelRequest.getUserId(), InitiativeGeneralDTO.BeneficiaryTypeEnum.PF, INITIATIVEID)
                .version(3L)
                .updateDate(LocalDateTime.now().minusDays(10L))
                .exhaustedBudget(false)
                .trxNumber(3L)
                .totalReward(BigDecimal.valueOf(100L))
                .totalAmount(BigDecimal.valueOf(100L)).build();
    }
}