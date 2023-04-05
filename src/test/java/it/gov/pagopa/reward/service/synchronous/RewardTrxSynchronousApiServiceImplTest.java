package it.gov.pagopa.reward.service.synchronous;

import it.gov.pagopa.reward.dto.mapper.RewardTransaction2PreviewResponseMapper;
import it.gov.pagopa.reward.dto.mapper.TransactionPreviewRequest2TransactionDTOMapper;
import it.gov.pagopa.reward.dto.synchronous.TransactionSynchronousRequest;
import it.gov.pagopa.reward.dto.synchronous.TransactionSynchronousResponse;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.exception.ClientExceptionWithBody;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.reward.OnboardedInitiativesService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionPreviewRequestFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    OnboardedInitiativesService onboardedInitiativesServiceMock;
    @Mock
    InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeServiceMock;
    @Mock
    UserInitiativeCountersRepository userInitiativeCountersRepositoryMock;
    @Mock
    TransactionPreviewRequest2TransactionDTOMapper transactionPreviewRequest2TransactionDTOMapperMock;
    @Mock
    RewardTransaction2PreviewResponseMapper rewardTransaction2PreviewResponseMapperMock;


    @Test
    void postTransactionPreviewError(){
        // Given
        TransactionSynchronousRequest previewRequest = TransactionPreviewRequestFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        String errorMessage = "User not onboarded to initiative %s".formatted(initiativeId);

        TransactionDTO transactionDTOMock = TransactionDTOFaker.mockInstance(1);
        Mockito.when(transactionPreviewRequest2TransactionDTOMapperMock.apply(Mockito.same(previewRequest))).thenReturn(transactionDTOMock);

        Mockito.when(onboardedInitiativesServiceMock.isOnboarded(Mockito.same(transactionDTOMock.getHpan()), Mockito.same(transactionDTOMock.getTrxDate()), Mockito.same(initiativeId))).thenReturn(Mono.just(Boolean.FALSE));

        RewardTrxSynchronousApiServiceImpl rewardTrxSynchronousApiApiService = new RewardTrxSynchronousApiServiceImpl(onboardedInitiativesServiceMock,initiativesEvaluatorFacadeServiceMock, userInitiativeCountersRepositoryMock, transactionPreviewRequest2TransactionDTOMapperMock, rewardTransaction2PreviewResponseMapperMock);

        // When
        try {
            rewardTrxSynchronousApiApiService.postTransactionPreview(previewRequest, initiativeId).block();

        } catch (Exception e){
            Assertions.assertTrue(e instanceof IllegalArgumentException);
            Assertions.assertEquals(errorMessage, e.getMessage());
        }
    }

    @Test
    void postTransactionPreviewOK(){
        // Given
        TransactionSynchronousRequest previewRequest = TransactionPreviewRequestFaker.mockInstance(1);
        String initiativeId = "INITIATIVEID";

        TransactionDTO transactionDTOMock = TransactionDTOFaker.mockInstance(1);
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(1);
        Mockito.when(onboardedInitiativesServiceMock.isOnboarded(Mockito.same(transactionDTOMock.getHpan()), Mockito.same(transactionDTOMock.getTrxDate()), Mockito.same(initiativeId))).thenReturn(Mono.just(Boolean.TRUE));
        Mockito.when(transactionPreviewRequest2TransactionDTOMapperMock.apply(Mockito.same(previewRequest))).thenReturn(transactionDTOMock);

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters();
        userInitiativeCounters.setUserId(transactionDTOMock.getUserId());
        Mockito.when(userInitiativeCountersRepositoryMock.findById(Mockito.anyString())).thenReturn(Mono.just(userInitiativeCounters));

        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTOFaker.mockInstance(1);
        UserInitiativeCountersWrapper userInitiativeCountersWrapper = UserInitiativeCountersWrapper.builder()
                .userId(userInitiativeCounters.getUserId())
                .initiatives(Map.of(initiativeId,userInitiativeCounters))
                .build();
        Pair<UserInitiativeCountersWrapper, RewardTransactionDTO> pair = Pair.of(userInitiativeCountersWrapper, rewardTransactionDTO);
        Mockito.when(initiativesEvaluatorFacadeServiceMock.evaluateInitiativesBudgetAndRules(transactionDTOMock, List.of(initiativeId), userInitiativeCountersWrapper)).thenReturn(pair);

        TransactionSynchronousResponse transactionSynchronousResponse = TransactionSynchronousResponse.builder()
                .initiativeId(initiativeId)
                .status(RewardConstants.REWARD_STATE_REWARDED)
                .build();
        Mockito.when(rewardTransaction2PreviewResponseMapperMock.apply(Mockito.same(previewRequest.getTransactionId()), Mockito.same(initiativeId), Mockito.same(rewardTransactionDTO))).thenReturn(transactionSynchronousResponse);

        RewardTrxSynchronousApiServiceImpl rewardTrxSynchronousApiApiService = new RewardTrxSynchronousApiServiceImpl(onboardedInitiativesServiceMock,initiativesEvaluatorFacadeServiceMock, userInitiativeCountersRepositoryMock, transactionPreviewRequest2TransactionDTOMapperMock, rewardTransaction2PreviewResponseMapperMock);

        // When
        TransactionSynchronousResponse result = rewardTrxSynchronousApiApiService.postTransactionPreview(previewRequest, initiativeId).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(transactionSynchronousResponse, result);
        Mockito.verify(transactionPreviewRequest2TransactionDTOMapperMock, Mockito.only()).apply(Mockito.any());
        Mockito.verify(onboardedInitiativesServiceMock, Mockito.only()).isOnboarded(Mockito.anyString(), Mockito.any(), Mockito.anyString());
        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.only()).findById(Mockito.anyString());
        Mockito.verify(initiativesEvaluatorFacadeServiceMock, Mockito.only()).evaluateInitiativesBudgetAndRules(Mockito.any(),Mockito.any(),Mockito.any());
        Mockito.verify(rewardTransaction2PreviewResponseMapperMock, Mockito.only()).apply(Mockito.any(), Mockito.any(), Mockito.any());

    }
}