package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;

@ExtendWith(MockitoExtension.class)
class RewardCalculatorMediatorServiceImplTest {

    @BeforeAll
    public static void setDefaultTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
    }

    @Test
    void execute() {
        // Given
        OnboardedInitiativesService onboardedInitiativesService = Mockito.mock(OnboardedInitiativesServiceImpl.class);
        UserInitiativeCountersRepository userInitiativeCountersRepository = Mockito.mock(UserInitiativeCountersRepository.class);
        UserInitiativeCountersUpdateService userInitiativeCountersUpdateService = Mockito.mock(UserInitiativeCountersUpdateService.class);
        InitiativesEvaluatorService initiativesEvaluatorService = Mockito.mock(InitiativesEvaluatorServiceImpl.class);
        ErrorNotifierService errorNotifierServiceMock = Mockito.mock(ErrorNotifierService.class);

        RewardCalculatorMediatorService rewardCalculatorMediatorService = new RewardCalculatorMediatorServiceImpl(onboardedInitiativesService, userInitiativeCountersRepository, initiativesEvaluatorService, userInitiativeCountersUpdateService, errorNotifierServiceMock, TestUtils.objectMapper);

        TransactionDTO trx1 = TransactionDTOFaker.mockInstance(0);
        TransactionDTO trx2 = TransactionDTOFaker.mockInstance(1);
        Flux<Message<String>> trxFlux = Flux.just(trx1, trx2)
                .map(TestUtils::jsonSerializer)
                .map(MessageBuilder::withPayload).map(MessageBuilder::build);

        Mockito.when(userInitiativeCountersRepository.findById(Mockito.<String>any())).thenReturn(Mono.empty());
        Mockito.when(userInitiativeCountersRepository.save(Mockito.any())).thenReturn(Mono.empty());

        List<String> initiatives1 = List.of("INITIATIVE1");
        Mockito.when(onboardedInitiativesService.getInitiatives(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromIterable(initiatives1));

        RewardTransactionDTO rTrx1 = Mockito.mock(RewardTransactionDTO.class);
        Mockito.when(initiativesEvaluatorService.evaluateInitiativesBudgetAndRules(Mockito.eq(trx1), Mockito.eq(initiatives1), Mockito.any()))
                .thenReturn(rTrx1);

        // When
        List<RewardTransactionDTO> result = rewardCalculatorMediatorService.execute(trxFlux).collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());

        Mockito.verifyNoInteractions(errorNotifierServiceMock);

        Mockito.verify(userInitiativeCountersRepository).save(Mockito.any());
    }
}