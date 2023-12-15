package it.gov.pagopa.reward.service.synchronous.op;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.mapper.trx.sync.RewardTransaction2SynchronousTransactionResponseDTOMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.SynchronousTransactionRequestDTO2TrxDtoOrResponseMapperTest;
import it.gov.pagopa.reward.dto.mapper.trx.sync.SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.TransactionProcessed2SyncTrxResponseDTOMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.InitiativeRewardType;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.exception.custom.InitiativeNotActiveException;
import it.gov.pagopa.reward.exception.custom.InitiativeNotFoundOrNotDiscountException;
import it.gov.pagopa.reward.exception.custom.InitiativeNotInContainerException;
import it.gov.pagopa.reward.exception.custom.RewardCalculatorConflictException;
import it.gov.pagopa.reward.model.BaseOnboardingInfo;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.reward.OnboardedInitiativesService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.service.synchronous.op.recover.HandleSyncCounterUpdatingTrxService;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionRequestDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode;
@ExtendWith(MockitoExtension.class)
class CreateTrxSynchronousServiceImplTest {

    @Mock
    private RewardContextHolderService rewardContextHolderServiceMock;
    @Mock
    private OnboardedInitiativesService onboardedInitiativesServiceMock;
    @Mock
    private InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeServiceMock;
    @Mock
    private TransactionProcessedRepository transactionProcessedRepositoryMock;
    @Mock
    private UserInitiativeCountersRepository userInitiativeCountersRepositoryMock;
    @Mock
    private HandleSyncCounterUpdatingTrxService handleSyncCounterUpdatingTrxServiceMock;

    private final SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper synchronousTransactionRequestDTOt2TrxDtoOrResponseMapper = new SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper("00");
    private final RewardTransaction2SynchronousTransactionResponseDTOMapper rewardTransaction2SynchronousTransactionResponseDTOMapper = new RewardTransaction2SynchronousTransactionResponseDTOMapper();
    private final TransactionProcessed2SyncTrxResponseDTOMapper transactionProcessed2SyncTrxResponseDTOMapper = new TransactionProcessed2SyncTrxResponseDTOMapper();

    private CreateTrxSynchronousService service;

    @BeforeEach
    void init(){
        service = new CreateTrxSynchronousServiceImpl(rewardContextHolderServiceMock, onboardedInitiativesServiceMock, initiativesEvaluatorFacadeServiceMock, transactionProcessedRepositoryMock, userInitiativeCountersRepositoryMock, handleSyncCounterUpdatingTrxServiceMock, synchronousTransactionRequestDTOt2TrxDtoOrResponseMapper, rewardTransaction2SynchronousTransactionResponseDTOMapper, transactionProcessed2SyncTrxResponseDTOMapper);
    }

    //region preview
    @Test
    void postTransactionPreviewNotOnboarded(){
        // Given
        SynchronousTransactionRequestDTO previewRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .initiativeRewardType(InitiativeRewardType.DISCOUNT)
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));
        Mockito.when(rewardContextHolderServiceMock.getRewardRulesKieInitiativeIds()).thenReturn(Set.of(initiativeId));
        Mockito.when(onboardedInitiativesServiceMock.isOnboarded(Mockito.eq(SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper
                .getPaymentInstrument(previewRequest.getUserId(), previewRequest.getChannel())), Mockito.same(previewRequest.getTrxDate()),
                Mockito.same(initiativeId))).thenReturn(Mono.empty());

        // When
        try {
            service.previewTransaction(previewRequest, initiativeId).block();
            Assertions.fail("Expected an Exception");

        } catch (Exception e){
            Assertions.assertInstanceOf(InitiativeNotActiveException.class, e);
            SynchronousTransactionResponseDTO resultResponse = ((InitiativeNotActiveException) e).getResponse();
            Assertions.assertNotNull(resultResponse);
            SynchronousTransactionRequestDTO2TrxDtoOrResponseMapperTest.errorResponseCommonAssertions(previewRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE), resultResponse);
        }
    }

    @Test
    void postTransactionPreviewInitiativeNotFound(){
        // Given
        SynchronousTransactionRequestDTO previewRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.empty());

        // When
        try {
            service.previewTransaction(previewRequest, initiativeId).block();
            Assertions.fail("Expected an Exception");

        } catch (Exception e){
            Assertions.assertInstanceOf(InitiativeNotFoundOrNotDiscountException.class, e);
            SynchronousTransactionResponseDTO resultResponse = ((InitiativeNotFoundOrNotDiscountException) e).getResponse();
            Assertions.assertNotNull(resultResponse);
            SynchronousTransactionRequestDTO2TrxDtoOrResponseMapperTest.errorResponseCommonAssertions(previewRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND), resultResponse);

        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void postTransactionPreviewOK(boolean existUserInitiativeCounter){
        // Given
        SynchronousTransactionRequestDTO previewRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .initiativeRewardType(InitiativeRewardType.DISCOUNT)
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));
        Mockito.when(rewardContextHolderServiceMock.getRewardRulesKieInitiativeIds()).thenReturn(Set.of(initiativeId));

        Mockito.when(onboardedInitiativesServiceMock.isOnboarded(Mockito.eq(SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper
                .getPaymentInstrument(previewRequest.getUserId(), previewRequest.getChannel())),
                Mockito.same(previewRequest.getTrxDate()),
                Mockito.same(initiativeId))).thenReturn(Mono.just(Pair.of(initiativeConfig,new BaseOnboardingInfo(initiativeId, null))));

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters();
        userInitiativeCounters.setId(UserInitiativeCounters.buildId(previewRequest.getUserId(), initiativeId));
        userInitiativeCounters.setEntityId(previewRequest.getUserId());
        userInitiativeCounters.setInitiativeId(initiativeId);
        Mockito.when(userInitiativeCountersRepositoryMock.findById(Mockito.anyString())).thenReturn(existUserInitiativeCounter ? Mono.just(userInitiativeCounters) : Mono.empty());

        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTOFaker.mockInstance(1);
        UserInitiativeCountersWrapper userInitiativeCountersWrapper = UserInitiativeCountersWrapper.builder()
                .entityId(userInitiativeCounters.getEntityId())
                .initiatives(Map.of(initiativeId,userInitiativeCounters))
                .build();
        Pair<UserInitiativeCountersWrapper, RewardTransactionDTO> pair = Pair.of(userInitiativeCountersWrapper, rewardTransactionDTO);
        Mockito.when(initiativesEvaluatorFacadeServiceMock.evaluateInitiativesBudgetAndRules(Mockito.argThat(t->t.getId().equals(previewRequest.getTransactionId())), Mockito.eq(List.of(initiativeId)), Mockito.any())).thenReturn(Mono.just(pair));

        SynchronousTransactionResponseDTO expectedResult = SynchronousTransactionResponseDTO.builder()
                .transactionId(previewRequest.getTransactionId())
                .userId(previewRequest.getUserId())
                .initiativeId(initiativeId)
                .channel(RewardConstants.TRX_CHANNEL_RTD)
                .operationType(OperationType.CHARGE)
                .amountCents(rewardTransactionDTO.getAmountCents())
                .amount(CommonUtilities.centsToEuro(rewardTransactionDTO.getAmountCents()))
                .effectiveAmount(CommonUtilities.centsToEuro(rewardTransactionDTO.getAmountCents()))
                .status(RewardConstants.REWARD_STATE_REWARDED)
                .build();

        // When
        SynchronousTransactionResponseDTO result = service.previewTransaction(previewRequest, initiativeId).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedResult, result);
        Mockito.verify(onboardedInitiativesServiceMock, Mockito.only()).isOnboarded(Mockito.anyString(), Mockito.any(), Mockito.anyString());
        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.only()).findById(Mockito.anyString());
        Mockito.verify(initiativesEvaluatorFacadeServiceMock, Mockito.only()).evaluateInitiativesBudgetAndRules(Mockito.any(),Mockito.any(),Mockito.any());
    }
    @Test
    void postTransactionPreviewNotInContainer(){
        // Given
        SynchronousTransactionRequestDTO previewRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .initiativeRewardType(InitiativeRewardType.DISCOUNT)
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));
        Mockito.when(rewardContextHolderServiceMock.getRewardRulesKieInitiativeIds()).thenReturn(Collections.emptySet());

        // When
        try {
            service.previewTransaction(previewRequest, initiativeId).block();
            Assertions.fail("Expected an Exception");

        } catch (Exception e){
            Assertions.assertInstanceOf(InitiativeNotInContainerException.class, e);
            SynchronousTransactionResponseDTO resultResponse = ((InitiativeNotInContainerException) e).getResponse();
            Assertions.assertNotNull(resultResponse);
            SynchronousTransactionRequestDTO2TrxDtoOrResponseMapperTest.errorResponseCommonAssertions(previewRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_RULE_ENGINE_NOT_READY), resultResponse);
        }
    }

    //endregion

    //region authorize
    @Test
    void authorizeTransactionAlreadyProcessed() {
        //Given
        SynchronousTransactionRequestDTO authorizeRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);
        trx.setUserId(authorizeRequest.getUserId());
        trx.setId(authorizeRequest.getTransactionId());
        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .initiativeRewardType(InitiativeRewardType.DISCOUNT)
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));
        Mockito.when(rewardContextHolderServiceMock.getRewardRulesKieInitiativeIds()).thenReturn(Set.of(initiativeId));

        TransactionProcessed transactionProcessed = TransactionProcessed.builder()
                .id(trx.getId())
                .rewards(Map.of(
                                initiativeId,
                                new Reward(initiativeId,"ORGANIZATION_"+initiativeId, BigDecimal.valueOf(20))))
                .userId(trx.getUserId())
                .build();
        Mockito.when(transactionProcessedRepositoryMock.findById(trx.getId())).thenReturn(Mono.just(transactionProcessed));

        SynchronousTransactionResponseDTO responseExpected =
                transactionProcessed2SyncTrxResponseDTOMapper.apply(transactionProcessed, initiativeId);

        Mono<UserInitiativeCounters> counterMono = Mono.just(new UserInitiativeCounters(trx.getUserId(), InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId));

        Mockito.when(userInitiativeCountersRepositoryMock.findById(UserInitiativeCounters.buildId(trx.getUserId(), initiativeId)))
                .thenReturn(counterMono);

        // When
        try {
            service.authorizeTransaction(authorizeRequest, initiativeId).block();
            Assertions.fail("Expected an Exception");

        } catch (Exception e){
            Assertions.assertInstanceOf(RewardCalculatorConflictException.class, e);
            SynchronousTransactionResponseDTO resultResponse = ((RewardCalculatorConflictException) e).getResponse();
            Assertions.assertNotNull(resultResponse);
            Assertions.assertEquals(responseExpected, resultResponse);
            Assertions.assertEquals(ExceptionCode.CONFLICT_ERROR,((RewardCalculatorConflictException) e).getCode());
        }

        // When
        try {
            service.authorizeTransaction(authorizeRequest, initiativeId).block();
            Assertions.fail("Expected an Exception");

        } catch (Exception e){
            Assertions.assertInstanceOf(RewardCalculatorConflictException.class, e);
            SynchronousTransactionResponseDTO resultResponse = ((RewardCalculatorConflictException) e).getResponse();
            Assertions.assertNotNull(resultResponse);
            Assertions.assertEquals(responseExpected, resultResponse);
            Assertions.assertEquals(ExceptionCode.CONFLICT_ERROR,((RewardCalculatorConflictException) e).getCode());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "false,false",
            "false,true",
            "true,false",
            "true,true"
    })
    void authorizeTransactionNotAlreadyProcessed(boolean existUserInitiativeCounter, boolean previousStuck) {
        //Given
        SynchronousTransactionRequestDTO authorizeRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .initiativeRewardType(InitiativeRewardType.DISCOUNT)
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));
        Mockito.when(rewardContextHolderServiceMock.getRewardRulesKieInitiativeIds()).thenReturn(Set.of(initiativeId));

        Mono<BaseTransactionProcessed> previousTrxProcessMono;
        if(previousStuck){
            TransactionProcessed trxProcessed = TransactionProcessed.builder()
                    .id(authorizeRequest.getTransactionId())
                    .rewards(Map.of(
                                    initiativeId,
                                    new Reward(initiativeId,"ORGANIZATION_"+initiativeId, BigDecimal.valueOf(20))))
                    .userId(authorizeRequest.getUserId())
                    .build();
            previousTrxProcessMono = Mono.just(trxProcessed);
        } else {
            previousTrxProcessMono = Mono.empty();
        }
        Mockito.when(transactionProcessedRepositoryMock.findById(authorizeRequest.getTransactionId())).thenReturn(previousTrxProcessMono);

        Mockito.when(onboardedInitiativesServiceMock.isOnboarded(Mockito.eq(SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper
                .getPaymentInstrument(authorizeRequest.getUserId(), authorizeRequest.getChannel())), Mockito.same(authorizeRequest.getTrxDate()),
                Mockito.same(initiativeId))).thenReturn(Mono.just(Pair.of(initiativeConfig,new BaseOnboardingInfo(initiativeId, null))));

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters();
        userInitiativeCounters.setId(UserInitiativeCounters.buildId(authorizeRequest.getUserId(), initiativeId));
        userInitiativeCounters.setEntityId(authorizeRequest.getUserId());
        userInitiativeCounters.setInitiativeId(initiativeId);
        if(previousStuck){
            userInitiativeCounters.setUpdatingTrxId(List.of(authorizeRequest.getTransactionId()));
            Mockito.when(userInitiativeCountersRepositoryMock.findById(userInitiativeCounters.getId())).thenReturn(Mono.just(userInitiativeCounters));
        }
        Mockito.when(userInitiativeCountersRepositoryMock.findByIdThrottled(userInitiativeCounters.getId(), authorizeRequest.getTransactionId())).thenReturn(existUserInitiativeCounter || previousStuck ? Mono.just(userInitiativeCounters) : Mono.empty());

        RewardTransactionDTO rewardTransaction = RewardTransactionDTOFaker.mockInstance(1);
        rewardTransaction.setId(authorizeRequest.getTransactionId());
        Mockito.when(initiativesEvaluatorFacadeServiceMock.evaluateAndUpdateBudget(Mockito.argThat(t->t.getId().equals(authorizeRequest.getTransactionId())),Mockito.eq(List.of(initiativeId)), Mockito.any())).thenReturn(Mono.just(rewardTransaction));

        SynchronousTransactionResponseDTO responseDTO = SynchronousTransactionResponseDTO.builder()
                .transactionId(authorizeRequest.getTransactionId())
                .userId(authorizeRequest.getUserId())
                .initiativeId(initiativeId)
                .channel(RewardConstants.TRX_CHANNEL_RTD)
                .operationType(OperationType.CHARGE)
                .amountCents(rewardTransaction.getAmountCents())
                .amount(CommonUtilities.centsToEuro(rewardTransaction.getAmountCents()))
                .effectiveAmount(CommonUtilities.centsToEuro(rewardTransaction.getAmountCents()))
                .status(RewardConstants.REWARD_STATE_REWARDED)
                .build();

        if(existUserInitiativeCounter || previousStuck) {
            Mockito.when(handleSyncCounterUpdatingTrxServiceMock.checkUpdatingTrx(Mockito.argThat(t->t.getId().equals(authorizeRequest.getTransactionId())), Mockito.eq(userInitiativeCounters)))
                    .thenReturn(Mono.just(userInitiativeCounters));
        }

        // When
        SynchronousTransactionResponseDTO result = service.authorizeTransaction(authorizeRequest, initiativeId).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(responseDTO, result);
    }


    @Test
    void authorizeTransactionInitiativeNotInContainer() {
        //Given
        SynchronousTransactionRequestDTO authorizeRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);
        trx.setUserId(authorizeRequest.getUserId());
        trx.setId(authorizeRequest.getTransactionId());
        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .initiativeRewardType(InitiativeRewardType.DISCOUNT)
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));
        Mockito.when(rewardContextHolderServiceMock.getRewardRulesKieInitiativeIds()).thenReturn(Collections.emptySet());

        // When
        try {
            service.authorizeTransaction(authorizeRequest, initiativeId).block();
            Assertions.fail("Expected an Exception");

        } catch (Exception e){
            Assertions.assertInstanceOf(InitiativeNotInContainerException.class, e);
            SynchronousTransactionResponseDTO resultResponse = ((InitiativeNotInContainerException) e).getResponse();
            Assertions.assertNotNull(resultResponse);
            SynchronousTransactionRequestDTO2TrxDtoOrResponseMapperTest.errorResponseCommonAssertions(authorizeRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_RULE_ENGINE_NOT_READY), resultResponse);
        }
    }
//end region
}