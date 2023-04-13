package it.gov.pagopa.reward.service.synchronous;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.mapper.RewardTransaction2SynchronousTransactionResponseDTOMapper;
import it.gov.pagopa.reward.dto.mapper.SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper;
import it.gov.pagopa.reward.dto.mapper.SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapperTest;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.exception.TransactionSynchronousException;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.reward.OnboardedInitiativesService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.service.reward.trx.TransactionProcessedService;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionRequestDTOFaker;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionResponseDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class RewardTrxSynchronousApiServiceImplTest {

    @Mock
    RewardContextHolderService rewardContextHolderServiceMock;
    @Mock
    OnboardedInitiativesService onboardedInitiativesServiceMock;
    @Mock
    InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeServiceMock;
    @Mock
    TransactionProcessedService transactionProcessedService;
    @Mock
    UserInitiativeCountersRepository userInitiativeCountersRepositoryMock;
    @Mock
    SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper synchronousTransactionRequestDTOt2TrxDtoOrResponseMapperMock;
    @Mock
    RewardTransaction2SynchronousTransactionResponseDTOMapper rewardTransaction2SynchronousTransactionResponseDTOMapperMock;


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
                        .status(RewardConstants.REWARD_STATE_REJECTED)
                        .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                        .build();
        Mockito.when(synchronousTransactionRequestDTOt2TrxDtoOrResponseMapperMock.apply(previewRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))).thenReturn(response);
        RewardTrxSynchronousApiServiceImpl rewardTrxSynchronousApiApiService = new RewardTrxSynchronousApiServiceImpl(rewardContextHolderServiceMock, onboardedInitiativesServiceMock,initiativesEvaluatorFacadeServiceMock, transactionProcessedService, userInitiativeCountersRepositoryMock, synchronousTransactionRequestDTOt2TrxDtoOrResponseMapperMock, rewardTransaction2SynchronousTransactionResponseDTOMapperMock);

        // When
        try {
            rewardTrxSynchronousApiApiService.previewTransaction(previewRequest, initiativeId).block();
            Assertions.fail("Expected an Exception");

        } catch (Exception e){
            Assertions.assertTrue(e instanceof TransactionSynchronousException);
            SynchronousTransactionResponseDTO resultResponse = ((TransactionSynchronousException) e).getResponse();
            Assertions.assertNotNull(resultResponse);
            SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapperTest.errorResponseCommonAssertions(previewRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE), resultResponse);
        }
    }

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
                .status(RewardConstants.REWARD_STATE_REJECTED)
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .build();
        Mockito.when(synchronousTransactionRequestDTOt2TrxDtoOrResponseMapperMock.apply(previewRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND))).thenReturn(response);

        RewardTrxSynchronousApiServiceImpl rewardTrxSynchronousApiApiService = new RewardTrxSynchronousApiServiceImpl(rewardContextHolderServiceMock, onboardedInitiativesServiceMock,initiativesEvaluatorFacadeServiceMock, transactionProcessedService, userInitiativeCountersRepositoryMock, synchronousTransactionRequestDTOt2TrxDtoOrResponseMapperMock, rewardTransaction2SynchronousTransactionResponseDTOMapperMock);

        // When
        try {
            rewardTrxSynchronousApiApiService.previewTransaction(previewRequest, initiativeId).block();
            Assertions.fail("Expected an Exception");

        } catch (Exception e){
            Assertions.assertTrue(e instanceof TransactionSynchronousException);
            SynchronousTransactionResponseDTO resultResponse = ((TransactionSynchronousException) e).getResponse();
            Assertions.assertNotNull(resultResponse);
            SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapperTest.errorResponseCommonAssertions(previewRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE), resultResponse);

        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void postTransactionPreviewOK(boolean existUserInitiativeCounter){
        // Given
        SynchronousTransactionRequestDTO previewRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        TransactionDTO transactionDTOMock = TransactionDTOFaker.mockInstance(1);
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
        Mockito.when(initiativesEvaluatorFacadeServiceMock.evaluateInitiativesBudgetAndRules(transactionDTOMock, List.of(initiativeId), userInitiativeCountersWrapper)).thenReturn(pair);

        SynchronousTransactionResponseDTO synchronousTransactionResponseDTO = SynchronousTransactionResponseDTO.builder()
                .initiativeId(initiativeId)
                .status(RewardConstants.REWARD_STATE_REWARDED)
                .build();
        Mockito.when(rewardTransaction2SynchronousTransactionResponseDTOMapperMock.apply(Mockito.same(previewRequest.getTransactionId()), Mockito.same(initiativeId), Mockito.same(rewardTransactionDTO))).thenReturn(synchronousTransactionResponseDTO);

        RewardTrxSynchronousApiServiceImpl rewardTrxSynchronousApiApiService = new RewardTrxSynchronousApiServiceImpl(rewardContextHolderServiceMock, onboardedInitiativesServiceMock,initiativesEvaluatorFacadeServiceMock, transactionProcessedService, userInitiativeCountersRepositoryMock, synchronousTransactionRequestDTOt2TrxDtoOrResponseMapperMock, rewardTransaction2SynchronousTransactionResponseDTOMapperMock);

        // When
        SynchronousTransactionResponseDTO result = rewardTrxSynchronousApiApiService.previewTransaction(previewRequest, initiativeId).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(synchronousTransactionResponseDTO, result);
        Mockito.verify(synchronousTransactionRequestDTOt2TrxDtoOrResponseMapperMock, Mockito.only()).apply(Mockito.any());
        Mockito.verify(onboardedInitiativesServiceMock, Mockito.only()).isOnboarded(Mockito.anyString(), Mockito.any(), Mockito.anyString());
        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.only()).findById(Mockito.anyString());
        Mockito.verify(initiativesEvaluatorFacadeServiceMock, Mockito.only()).evaluateInitiativesBudgetAndRules(Mockito.any(),Mockito.any(),Mockito.any());
        Mockito.verify(rewardTransaction2SynchronousTransactionResponseDTOMapperMock, Mockito.only()).apply(Mockito.any(), Mockito.any(), Mockito.any());
    }
}