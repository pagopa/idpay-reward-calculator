package it.gov.pagopa.reward.service.synchronous.op;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.RewardTransaction2SynchronousTransactionResponseDTOMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.SynchronousTransactionRequestDTO2TrxDtoOrResponseMapperTest;
import it.gov.pagopa.reward.dto.mapper.trx.sync.SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionAuthRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.InitiativeRewardType;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.exception.custom.*;
import it.gov.pagopa.reward.model.BaseOnboardingInfo;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.reward.OnboardedInitiativesService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateService;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionAuthRequestDTOFaker;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionRequestDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatcher;
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
import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionMessage;
@ExtendWith(MockitoExtension.class)
class CreateTrxSynchronousServiceImplTest {

    @Mock
    private RewardContextHolderService rewardContextHolderServiceMock;
    @Mock
    private OnboardedInitiativesService onboardedInitiativesServiceMock;
    @Mock
    private InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeServiceMock;
    @Mock
    private UserInitiativeCountersRepository userInitiativeCountersRepositoryMock;
    @Mock
    private UserInitiativeCountersUpdateService userInitiativeCountersUpdateServiceMock;

    private final SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper synchronousTransactionRequestDTOt2TrxDtoOrResponseMapper = new SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper("00");
    private final RewardTransaction2SynchronousTransactionResponseDTOMapper rewardTransaction2SynchronousTransactionResponseDTOMapper = new RewardTransaction2SynchronousTransactionResponseDTOMapper();
    private final Transaction2RewardTransactionMapper rewardTransactionMapper = new Transaction2RewardTransactionMapper();

    private CreateTrxSynchronousService service;

    @BeforeEach
    void init(){
        service = new CreateTrxSynchronousServiceImpl(
                rewardContextHolderServiceMock,
                onboardedInitiativesServiceMock,
                initiativesEvaluatorFacadeServiceMock,
                userInitiativeCountersRepositoryMock,
                synchronousTransactionRequestDTOt2TrxDtoOrResponseMapper,
                rewardTransaction2SynchronousTransactionResponseDTOMapper,
                rewardTransactionMapper,
                userInitiativeCountersUpdateServiceMock);
    }

    @AfterEach
    void verifyNotMoreMockInvocations(){
        Mockito.verifyNoMoreInteractions(
                rewardContextHolderServiceMock,
                onboardedInitiativesServiceMock,
                initiativesEvaluatorFacadeServiceMock,
                userInitiativeCountersRepositoryMock,
                userInitiativeCountersUpdateServiceMock);
    }

    //region preview
    @Test
    void postTransactionPreviewNotOnboarded(){
        // Given
        SynchronousTransactionRequestDTO previewRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        mockRewardContextHolderService(initiativeId);
        Mockito.when(onboardedInitiativesServiceMock.isOnboarded(Mockito.eq(SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper
                        .getPaymentInstrument(previewRequest.getUserId(), previewRequest.getChannel())), Mockito.same(previewRequest.getTrxDate()),
                Mockito.same(initiativeId))).thenReturn(Mono.empty());

        Mono<SynchronousTransactionResponseDTO> mono = service.previewTransaction(previewRequest, initiativeId);

        InitiativeNotActiveException resultException = Assertions.assertThrows(InitiativeNotActiveException.class, mono::block);

        ServiceExceptionPayload resultResponse = resultException.getPayload();
        Assertions.assertInstanceOf(SynchronousTransactionResponseDTO.class, resultResponse);
        Assertions.assertNotNull(resultResponse);
        Assertions.assertEquals(ExceptionCode.INITIATIVE_NOT_ACTIVE_FOR_USER,resultException.getCode());
        SynchronousTransactionRequestDTO2TrxDtoOrResponseMapperTest.errorResponseCommonAssertions(previewRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE), (SynchronousTransactionResponseDTO) resultResponse);

    }

    @Test
    void postTransactionPreviewInitiativeNotFound(){
        // Given
        SynchronousTransactionRequestDTO previewRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.empty());

        Mono<SynchronousTransactionResponseDTO> mono = service.previewTransaction(previewRequest, initiativeId);
        InitiativeNotFoundOrNotDiscountException resultException = Assertions.assertThrows(InitiativeNotFoundOrNotDiscountException.class, mono::block);

        ServiceExceptionPayload resultResponse = resultException.getPayload();
        Assertions.assertInstanceOf(SynchronousTransactionResponseDTO.class, resultResponse);
        Assertions.assertNotNull(resultResponse);
        Assertions.assertEquals(ExceptionCode.INITIATIVE_NOT_FOUND_OR_NOT_DISCOUNT,resultException.getCode());
        SynchronousTransactionRequestDTO2TrxDtoOrResponseMapperTest.errorResponseCommonAssertions(previewRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND), (SynchronousTransactionResponseDTO) resultResponse);

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void postTransactionPreviewOK(boolean existUserInitiativeCounter){
        // Given
        SynchronousTransactionRequestDTO previewRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        InitiativeConfig initiativeConfig = mockRewardContextHolderService(initiativeId);

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

        Mono<SynchronousTransactionResponseDTO> mono = service.previewTransaction(previewRequest, initiativeId);
        InitiativeNotInContainerException resultException = Assertions.assertThrows(InitiativeNotInContainerException.class, mono::block);

        ServiceExceptionPayload resultResponse = resultException.getPayload();
        Assertions.assertInstanceOf(SynchronousTransactionResponseDTO.class, resultResponse);
        Assertions.assertNotNull(resultResponse);
        Assertions.assertEquals(ExceptionCode.INITIATIVE_NOT_READY,resultException.getCode());
        SynchronousTransactionRequestDTO2TrxDtoOrResponseMapperTest.errorResponseCommonAssertions(previewRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_RULE_ENGINE_NOT_READY), (SynchronousTransactionResponseDTO) resultResponse);

    }

    //endregion

    //region authorize
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void authorizeTransaction_GivenPendingCounterThenThrowPendingCounterException(boolean pendingRefund){
        //Given
        SynchronousTransactionAuthRequestDTO authorizeRequest = SynchronousTransactionAuthRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";
        long counterVersion = 3L;

        InitiativeConfig initiativeConfig = mockRewardContextHolderService(initiativeId);
        mockOnboardedInitiativeService(authorizeRequest, initiativeConfig);
        mockUserInitiativeCountersRepositoryFind(authorizeRequest, initiativeId, counterVersion-1,
                pendingRefund
                ? TransactionDTOFaker.mockInstanceBuilder(1)
                        .id(authorizeRequest.getTransactionId())
                        .operationTypeTranscoded(OperationType.REFUND)
                        .build()
                : TransactionDTOFaker.mockInstance(2));

        // When
        Mono<SynchronousTransactionResponseDTO> mono = service.authorizeTransaction(authorizeRequest, initiativeId, counterVersion);
        PendingCounterException result = Assertions.assertThrows(PendingCounterException.class, mono::block);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ExceptionCode.PENDING_COUNTER, result.getCode());
        Assertions.assertEquals(ExceptionMessage.PENDING_COUNTER, result.getMessage());

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void authorizeTransaction_GivenInvalidCounterVersionAndInvalidRewardThenThrowInvalidCounterException(boolean stillRewarded){
        //Given
        SynchronousTransactionAuthRequestDTO authorizeRequest = SynchronousTransactionAuthRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";
        long counterVersion = 3L;

        InitiativeConfig initiativeConfig = mockRewardContextHolderService(initiativeId);
        mockOnboardedInitiativeService(authorizeRequest, initiativeConfig);
        UserInitiativeCounters counter = mockUserInitiativeCountersRepositoryFind(authorizeRequest, initiativeId, counterVersion, null);
        mockInitiativesEvaluatorFacadeService(authorizeRequest, initiativeConfig, counter,
                stillRewarded
                        ? CommonUtilities.centsToEuro(authorizeRequest.getRewardCents() - 10)
                        : null);

        // When
        Mono<SynchronousTransactionResponseDTO> mono = service.authorizeTransaction(authorizeRequest, initiativeId, counterVersion);
        InvalidCounterVersionException result = Assertions.assertThrows(InvalidCounterVersionException.class, mono::block);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ExceptionCode.INVALID_COUNTER_VERSION, result.getCode());
        Assertions.assertEquals(ExceptionMessage.INVALID_COUNTER_VERSION, result.getMessage());
    }

    @Test
    void authorizeTransaction_GivenActualPendingThenOkButNotSaving(){
        //Given
        SynchronousTransactionAuthRequestDTO authorizeRequest = SynchronousTransactionAuthRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";
        long counterVersion = 3L;

        InitiativeConfig initiativeConfig = mockRewardContextHolderService(initiativeId);
        mockOnboardedInitiativeService(authorizeRequest, initiativeConfig);
        mockUserInitiativeCountersRepositoryFind(authorizeRequest, initiativeId, counterVersion-1,
                TransactionDTOFaker.mockInstanceBuilder(1)
                        .id(authorizeRequest.getTransactionId())
                        .operationTypeTranscoded(OperationType.CHARGE)
                        .build());

        SynchronousTransactionResponseDTO expectedResult = buildExpectedResponse(authorizeRequest, initiativeId, initiativeConfig);

        // When
        SynchronousTransactionResponseDTO result = service.authorizeTransaction(authorizeRequest, initiativeId, counterVersion).block();

        // Then
        Assertions.assertEquals(expectedResult, result);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void authorizeTransaction_GivenValidRequestThenOk(boolean counterVersionMatch){
        //Given
        SynchronousTransactionAuthRequestDTO authorizeRequest = SynchronousTransactionAuthRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";
        long counterVersion = 3L;
        long foundCounterVersion = counterVersionMatch
                ? counterVersion -1
                : counterVersion;

        InitiativeConfig initiativeConfig = mockRewardContextHolderService(initiativeId);
        mockOnboardedInitiativeService(authorizeRequest, initiativeConfig);
        UserInitiativeCounters counter = mockUserInitiativeCountersRepositoryFind(authorizeRequest, initiativeId, foundCounterVersion, null);

        if(!counterVersionMatch){
            mockInitiativesEvaluatorFacadeService(authorizeRequest, initiativeConfig, counter, CommonUtilities.centsToEuro(authorizeRequest.getRewardCents()));
        } else {
            mockUserInitiativeCountersUpdateService(authorizeRequest, initiativeConfig, counter, CommonUtilities.centsToEuro(authorizeRequest.getRewardCents()));
        }

        mockUserInitiativeCountersRepositorySave(counter);

        SynchronousTransactionResponseDTO expectedResult = buildExpectedResponse(authorizeRequest, initiativeId, initiativeConfig);

        // When
        SynchronousTransactionResponseDTO result = service.authorizeTransaction(authorizeRequest, initiativeId, counterVersion).block();

        // Then
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    void authorizeTransactionInitiativeNotInContainer() {
        //Given
        SynchronousTransactionAuthRequestDTO authorizeRequest = SynchronousTransactionAuthRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";
        long counterVersion = 3L;

        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);
        trx.setUserId(authorizeRequest.getUserId());
        trx.setId(authorizeRequest.getTransactionId());
        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .initiativeRewardType(InitiativeRewardType.DISCOUNT)
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));
        Mockito.when(rewardContextHolderServiceMock.getRewardRulesKieInitiativeIds()).thenReturn(Collections.emptySet());

        Mono<SynchronousTransactionResponseDTO> mono = service.authorizeTransaction(authorizeRequest, initiativeId, counterVersion);
        InitiativeNotInContainerException resultException = Assertions.assertThrows(InitiativeNotInContainerException.class, mono::block);

        ServiceExceptionPayload resultResponse = resultException.getPayload();
        Assertions.assertInstanceOf(SynchronousTransactionResponseDTO.class,resultResponse);
        Assertions.assertNotNull(resultResponse);
        Assertions.assertEquals(ExceptionCode.INITIATIVE_NOT_READY,resultException.getCode());
        Assertions.assertEquals(String.format(ExceptionMessage.INITIATIVE_NOT_READY_MSG,initiativeId),resultException.getMessage());
        SynchronousTransactionRequestDTO2TrxDtoOrResponseMapperTest.errorResponseCommonAssertions(authorizeRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_RULE_ENGINE_NOT_READY), (SynchronousTransactionResponseDTO) resultResponse);

    }
//end region

    private InitiativeConfig mockRewardContextHolderService(String initiativeId) {
        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .initiativeRewardType(InitiativeRewardType.DISCOUNT)
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));
        Mockito.when(rewardContextHolderServiceMock.getRewardRulesKieInitiativeIds()).thenReturn(Set.of(initiativeId));
        return initiativeConfig;
    }

    private void mockOnboardedInitiativeService(SynchronousTransactionAuthRequestDTO authorizeRequest, InitiativeConfig initiativeConfig) {
        Mockito.when(onboardedInitiativesServiceMock.isOnboarded(
                        Mockito.eq(SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper.getPaymentInstrument(authorizeRequest.getUserId(), authorizeRequest.getChannel())),
                        Mockito.same(authorizeRequest.getTrxChargeDate()),
                        Mockito.same(initiativeConfig.getInitiativeId())))
                .thenReturn(Mono.just(Pair.of(initiativeConfig,new BaseOnboardingInfo(initiativeConfig.getInitiativeId(), null))));
    }

    private UserInitiativeCounters mockUserInitiativeCountersRepositoryFind(SynchronousTransactionAuthRequestDTO authorizeRequest, String initiativeId, long counterVersion, TransactionDTO pendingTrx) {
        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters();
        userInitiativeCounters.setId(UserInitiativeCounters.buildId(authorizeRequest.getUserId(), initiativeId));
        userInitiativeCounters.setEntityId(authorizeRequest.getUserId());
        userInitiativeCounters.setInitiativeId(initiativeId);
        userInitiativeCounters.setVersion(counterVersion);
        userInitiativeCounters.setPendingTrx(pendingTrx);
        Mockito.when(userInitiativeCountersRepositoryMock.findById(userInitiativeCounters.getId())).thenReturn(Mono.just(userInitiativeCounters));
        return userInitiativeCounters;
    }

    private void mockInitiativesEvaluatorFacadeService(SynchronousTransactionRequestDTO syncTransactionRequest, InitiativeConfig initiativeConfig, UserInitiativeCounters counter, BigDecimal reward) {
        RewardTransactionDTO rewardTransaction = buildExpectedRewardedTrx(syncTransactionRequest, initiativeConfig, counter, reward);
        UserInitiativeCountersWrapper counters = new UserInitiativeCountersWrapper(
                syncTransactionRequest.getUserId(),
                Map.of(counter.getInitiativeId(), counter));

        Mockito.when(initiativesEvaluatorFacadeServiceMock.evaluateInitiativesBudgetAndRules(
                        synchronousTransactionRequestDTOt2TrxDtoOrResponseMapper.apply(syncTransactionRequest),
                        List.of(initiativeConfig.getInitiativeId()),
                        counters))
                .thenReturn(Mono.just(Pair.of(counters,rewardTransaction)));
    }

    private RewardTransactionDTO buildExpectedRewardedTrx(SynchronousTransactionRequestDTO syncTransactionRequest, InitiativeConfig initiativeConfig, UserInitiativeCounters counter, BigDecimal reward) {
        RewardTransactionDTO rewardTransaction =
                rewardTransactionMapper.apply(
                        synchronousTransactionRequestDTOt2TrxDtoOrResponseMapper.apply(syncTransactionRequest)
                );
        if(reward !=null){
            rewardTransaction.setInitiatives(List.of(initiativeConfig.getInitiativeId()));
            rewardTransaction.setRewards(Map.of(counter.getInitiativeId(), new Reward(initiativeConfig.getInitiativeId(), initiativeConfig.getOrganizationId(), reward)));
        } else {
            rewardTransaction.setRewards(Collections.emptyMap());
        }
        return rewardTransaction;
    }

    private ArgumentMatcher<RewardTransactionDTO> buildRewardTrxMatcher(RewardTransactionDTO rewardTransaction){
        return r -> {
            rewardTransaction.setElaborationDateTime(r.getElaborationDateTime());
            return r.equals(rewardTransaction);
        };
    }

    private void mockUserInitiativeCountersRepositorySave(UserInitiativeCounters counter) {
        Mockito.when(userInitiativeCountersRepositoryMock.saveIfVersionNotChanged(counter)).thenReturn(Mono.just(counter));
    }

    private void mockUserInitiativeCountersUpdateService(SynchronousTransactionRequestDTO transactionRequest, InitiativeConfig initiativeConfig, UserInitiativeCounters counter, BigDecimal reward) {
        RewardTransactionDTO rewardTransaction = buildExpectedRewardedTrx(transactionRequest, initiativeConfig, counter, reward);
        UserInitiativeCountersWrapper counters = new UserInitiativeCountersWrapper(
                transactionRequest.getUserId(),
                Map.of(counter.getInitiativeId(), counter));

        Mockito.when(userInitiativeCountersUpdateServiceMock.update(
                        Mockito.eq(counters),
                        Mockito.argThat(buildRewardTrxMatcher(rewardTransaction))))
                .thenReturn(Mono.just(rewardTransaction));
    }

    private SynchronousTransactionResponseDTO buildExpectedResponse(SynchronousTransactionAuthRequestDTO authorizeRequest, String initiativeId, InitiativeConfig initiativeConfig) {
        return SynchronousTransactionResponseDTO.builder()
                .transactionId(authorizeRequest.getTransactionId())
                .userId(authorizeRequest.getUserId())
                .initiativeId(initiativeId)
                .channel(authorizeRequest.getChannel())
                .operationType(OperationType.CHARGE)
                .amountCents(authorizeRequest.getAmountCents())
                .amount(CommonUtilities.centsToEuro(authorizeRequest.getAmountCents()))
                .effectiveAmount(CommonUtilities.centsToEuro(authorizeRequest.getAmountCents()))
                .status(RewardConstants.REWARD_STATE_REWARDED)
                .reward(new Reward(initiativeId, initiativeConfig.getOrganizationId(), CommonUtilities.centsToEuro(authorizeRequest.getRewardCents())))
                .build();
    }
}