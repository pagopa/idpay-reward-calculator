package it.gov.pagopa.reward.service.synchronous.op;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.mapper.trx.sync.RewardTransaction2SynchronousTransactionResponseDTOMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.SynchronousTransactionRequestDTO2TrxDtoOrResponseMapperTest;
import it.gov.pagopa.reward.dto.mapper.trx.sync.TransactionProcessed2SyncTrxResponseDTOMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.exception.TransactionSynchronousException;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.reward.OnboardedInitiativesService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionRequestDTOFaker;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionResponseDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

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
    private SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper synchronousTransactionRequestDTOt2TrxDtoOrResponseMapperMock;
    @Mock
    private RewardTransaction2SynchronousTransactionResponseDTOMapper rewardTransaction2SynchronousTransactionResponseDTOMapperMock;
    @Mock
    private TransactionProcessed2SyncTrxResponseDTOMapper transactionProcessed2SyncTrxResponseDTOMapperMock;

    private CreateTrxSynchronousService service;

    @BeforeEach
    void init(){
        service = new CreateTrxSynchronousServiceImpl(rewardContextHolderServiceMock, onboardedInitiativesServiceMock,initiativesEvaluatorFacadeServiceMock, transactionProcessedRepositoryMock, userInitiativeCountersRepositoryMock, synchronousTransactionRequestDTOt2TrxDtoOrResponseMapperMock, rewardTransaction2SynchronousTransactionResponseDTOMapperMock, transactionProcessed2SyncTrxResponseDTOMapperMock);
    }

    //region preview
    @Test
    void postTransactionPreviewNotOnboarded(){
        // Given
        SynchronousTransactionRequestDTO previewRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        TransactionDTO transactionDTOMock = TransactionDTOFaker.mockInstance(1);
        Mockito.when(synchronousTransactionRequestDTOt2TrxDtoOrResponseMapperMock.apply(Mockito.same(previewRequest))).thenReturn(transactionDTOMock);

        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));
        Mockito.when(onboardedInitiativesServiceMock.isOnboarded(Mockito.same(transactionDTOMock.getHpan()), Mockito.same(transactionDTOMock.getTrxDate()), Mockito.same(initiativeId))).thenReturn(Mono.just(Boolean.FALSE));

        SynchronousTransactionResponseDTO response = SynchronousTransactionResponseDTOFaker.mockInstanceBuilder(1)
                        .initiativeId(initiativeId)
                        .channel(previewRequest.getChannel())
                        .operationType(OperationType.CHARGE)
                        .amountCents(previewRequest.getAmountCents())
                        .amount(CommonUtilities.centsToEuro(previewRequest.getAmountCents()))
                        .effectiveAmount(CommonUtilities.centsToEuro(previewRequest.getAmountCents()))
                        .status(RewardConstants.REWARD_STATE_REJECTED)
                        .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                        .build();
        Mockito.when(synchronousTransactionRequestDTOt2TrxDtoOrResponseMapperMock.apply(previewRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))).thenReturn(response);

        // When
        try {
            service.previewTransaction(previewRequest, initiativeId).block();
            Assertions.fail("Expected an Exception");

        } catch (Exception e){
            Assertions.assertTrue(e instanceof TransactionSynchronousException);
            SynchronousTransactionResponseDTO resultResponse = ((TransactionSynchronousException) e).getResponse();
            Assertions.assertNotNull(resultResponse);
            SynchronousTransactionRequestDTO2TrxDtoOrResponseMapperTest.errorResponseCommonAssertions(previewRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE), resultResponse);
        }
    }

    //endregion

    @Test
    void postTransactionPreviewInitiativeNotFound(){
        // Given
        SynchronousTransactionRequestDTO previewRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        TransactionDTO transactionDTOMock = TransactionDTOFaker.mockInstance(1);
        Mockito.when(synchronousTransactionRequestDTOt2TrxDtoOrResponseMapperMock.apply(Mockito.same(previewRequest))).thenReturn(transactionDTOMock);

        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.empty());

        SynchronousTransactionResponseDTO response = SynchronousTransactionResponseDTOFaker.mockInstanceBuilder(1)
                .initiativeId(initiativeId)
                .channel("CHANNEL")
                .operationType(OperationType.CHARGE)
                .amountCents(previewRequest.getAmountCents())
                .amount(CommonUtilities.centsToEuro(previewRequest.getAmountCents()))
                .effectiveAmount(CommonUtilities.centsToEuro(previewRequest.getAmountCents()))
                .status(RewardConstants.REWARD_STATE_REJECTED)
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .build();
        Mockito.when(synchronousTransactionRequestDTOt2TrxDtoOrResponseMapperMock.apply(previewRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND))).thenReturn(response);

        // When
        try {
            service.previewTransaction(previewRequest, initiativeId).block();
            Assertions.fail("Expected an Exception");

        } catch (Exception e){
            Assertions.assertTrue(e instanceof TransactionSynchronousException);
            SynchronousTransactionResponseDTO resultResponse = ((TransactionSynchronousException) e).getResponse();
            Assertions.assertNotNull(resultResponse);
            SynchronousTransactionRequestDTO2TrxDtoOrResponseMapperTest.errorResponseCommonAssertions(previewRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE), resultResponse);

        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void postTransactionPreviewOK(boolean existUserInitiativeCounter){
        // Given
        SynchronousTransactionRequestDTO previewRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        TransactionDTO transactionDTOMock = TransactionDTOFaker.mockInstance(1);
        transactionDTOMock.setUserId(previewRequest.getUserId());
        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));

        Mockito.when(onboardedInitiativesServiceMock.isOnboarded(Mockito.same(transactionDTOMock.getHpan()), Mockito.same(transactionDTOMock.getTrxDate()), Mockito.same(initiativeId))).thenReturn(Mono.just(Boolean.TRUE));

        Mockito.when(synchronousTransactionRequestDTOt2TrxDtoOrResponseMapperMock.apply(Mockito.same(previewRequest))).thenReturn(transactionDTOMock);

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters();
        userInitiativeCounters.setId(UserInitiativeCounters.buildId(transactionDTOMock.getUserId(), initiativeId));
        userInitiativeCounters.setUserId(transactionDTOMock.getUserId());
        userInitiativeCounters.setInitiativeId(initiativeId);
        Mockito.when(userInitiativeCountersRepositoryMock.findById(Mockito.anyString())).thenReturn(existUserInitiativeCounter ? Mono.just(userInitiativeCounters) : Mono.empty());

        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTOFaker.mockInstance(1);
        UserInitiativeCountersWrapper userInitiativeCountersWrapper = UserInitiativeCountersWrapper.builder()
                .userId(userInitiativeCounters.getUserId())
                .initiatives(Map.of(initiativeId,userInitiativeCounters))
                .build();
        Pair<UserInitiativeCountersWrapper, RewardTransactionDTO> pair = Pair.of(userInitiativeCountersWrapper, rewardTransactionDTO);
        Mockito.when(initiativesEvaluatorFacadeServiceMock.evaluateInitiativesBudgetAndRules(Mockito.eq(transactionDTOMock), Mockito.eq(List.of(initiativeId)), Mockito.any())).thenReturn(Mono.just(pair));

        SynchronousTransactionResponseDTO expectedResult = SynchronousTransactionResponseDTO.builder()
                .initiativeId(initiativeId)
                .channel("channel")
                .operationType(OperationType.CHARGE)
                .amountCents(previewRequest.getAmountCents())
                .amount(CommonUtilities.centsToEuro(previewRequest.getAmountCents()))
                .effectiveAmount(CommonUtilities.centsToEuro(previewRequest.getAmountCents()))
                .status(RewardConstants.REWARD_STATE_REWARDED)
                .build();
        Mockito.when(rewardTransaction2SynchronousTransactionResponseDTOMapperMock.apply(Mockito.same(transactionDTOMock.getId()), Mockito.same(initiativeId), Mockito.same(rewardTransactionDTO))).thenReturn(expectedResult);

        // When
        SynchronousTransactionResponseDTO result = service.previewTransaction(previewRequest, initiativeId).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedResult, result);
        Mockito.verify(synchronousTransactionRequestDTOt2TrxDtoOrResponseMapperMock, Mockito.only()).apply(Mockito.any());
        Mockito.verify(onboardedInitiativesServiceMock, Mockito.only()).isOnboarded(Mockito.anyString(), Mockito.any(), Mockito.anyString());
        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.only()).findById(Mockito.anyString());
        Mockito.verify(initiativesEvaluatorFacadeServiceMock, Mockito.only()).evaluateInitiativesBudgetAndRules(Mockito.any(),Mockito.any(),Mockito.any());
        Mockito.verify(rewardTransaction2SynchronousTransactionResponseDTOMapperMock, Mockito.only()).apply(Mockito.any(), Mockito.any(), Mockito.any());
    }

    //region authorize
    @Test
    void authorizeTransactionAlreadyProcessed() {
        //Given
        SynchronousTransactionRequestDTO authorizeRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        TransactionDTO transactionDTOMock = TransactionDTOFaker.mockInstance(1);
        transactionDTOMock.setUserId(authorizeRequest.getUserId());
        transactionDTOMock.setId(authorizeRequest.getTransactionId());
        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));

        Mockito.when(synchronousTransactionRequestDTOt2TrxDtoOrResponseMapperMock.apply(Mockito.same(authorizeRequest))).thenReturn(transactionDTOMock);

        TransactionProcessed transactionProcessed = TransactionProcessed.builder()
                .id(transactionDTOMock.getId()).build();
        Mockito.when(transactionProcessedRepositoryMock.findById(transactionDTOMock.getId())).thenReturn(Mono.just(transactionProcessed));

        SynchronousTransactionResponseDTO responseExpected = SynchronousTransactionResponseDTO.builder()
                .transactionId(authorizeRequest.getTransactionId())
                .userId(transactionDTOMock.getUserId())
                .build();
        Mockito.when(transactionProcessed2SyncTrxResponseDTOMapperMock.apply(transactionProcessed,initiativeId)).thenReturn(responseExpected);

        // When
        try {
            service.authorizeTransaction(authorizeRequest, initiativeId).block();
            Assertions.fail("Expected an Exception");

        } catch (Exception e){
            Assertions.assertTrue(e instanceof TransactionSynchronousException, () -> {e.printStackTrace(); return "Unexpected exception";});
            SynchronousTransactionResponseDTO resultResponse = ((TransactionSynchronousException) e).getResponse();
            Assertions.assertNotNull(resultResponse);
            Assertions.assertEquals(responseExpected, resultResponse);
            Assertions.assertEquals(HttpStatus.CONFLICT,((TransactionSynchronousException) e).getHttpStatus());
        }


        // When
        try {
            service.authorizeTransaction(authorizeRequest, initiativeId).block();
            Assertions.fail("Expected an Exception");

        } catch (Exception e){
            Assertions.assertTrue(e instanceof TransactionSynchronousException);
            SynchronousTransactionResponseDTO resultResponse = ((TransactionSynchronousException) e).getResponse();
            Assertions.assertNotNull(resultResponse);
            Assertions.assertEquals(responseExpected, resultResponse);
            Assertions.assertEquals(HttpStatus.CONFLICT,((TransactionSynchronousException) e).getHttpStatus());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void authorizeTransactionNotAlreadyProcessed(boolean existUserInitiativeCounter) {
        //Given
        SynchronousTransactionRequestDTO authorizeRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        TransactionDTO transactionDTOMock = TransactionDTOFaker.mockInstance(1);
        transactionDTOMock.setId(authorizeRequest.getTransactionId());
        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .build();
        Mockito.when(synchronousTransactionRequestDTOt2TrxDtoOrResponseMapperMock.apply(authorizeRequest)).thenReturn(transactionDTOMock);
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));

        Mockito.when(transactionProcessedRepositoryMock.findById(transactionDTOMock.getId())).thenReturn(Mono.empty());

        Mockito.when(onboardedInitiativesServiceMock.isOnboarded(Mockito.same(transactionDTOMock.getHpan()), Mockito.same(transactionDTOMock.getTrxDate()), Mockito.same(initiativeId))).thenReturn(Mono.just(Boolean.TRUE));

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters();
        userInitiativeCounters.setId(UserInitiativeCounters.buildId(transactionDTOMock.getUserId(), initiativeId));
        userInitiativeCounters.setUserId(transactionDTOMock.getUserId());
        userInitiativeCounters.setInitiativeId(initiativeId);
        Mockito.when(userInitiativeCountersRepositoryMock.findByIdThrottled(Mockito.anyString())).thenReturn(existUserInitiativeCounter ? Mono.just(userInitiativeCounters) : Mono.empty());

        RewardTransactionDTO rewardTransaction = RewardTransactionDTOFaker.mockInstance(1);
        rewardTransaction.setId(authorizeRequest.getTransactionId());
        Mockito.when(initiativesEvaluatorFacadeServiceMock.evaluateAndUpdateBudget(Mockito.eq(transactionDTOMock),Mockito.eq(List.of(initiativeId)), Mockito.any())).thenReturn(Mono.just(rewardTransaction));

        SynchronousTransactionResponseDTO responseDTO = SynchronousTransactionResponseDTO.builder()
                .transactionId(authorizeRequest.getTransactionId())
                .build();
        Mockito.when(rewardTransaction2SynchronousTransactionResponseDTOMapperMock.apply(authorizeRequest.getTransactionId(), initiativeId, rewardTransaction)).thenReturn(responseDTO);

        // When
        SynchronousTransactionResponseDTO result = service.authorizeTransaction(authorizeRequest, initiativeId).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(responseDTO, result);
    }

    //end region
}