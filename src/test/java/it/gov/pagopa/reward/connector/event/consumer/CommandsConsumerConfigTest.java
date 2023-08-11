package it.gov.pagopa.reward.connector.event.consumer;

import com.mongodb.MongoException;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.commands.CommandOperationDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.enums.InitiativeRewardType;
import it.gov.pagopa.reward.model.*;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.test.fakers.TransactionProcessedFaker;
import it.gov.pagopa.reward.utils.CommandsConstants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.reward.service.commands.ops.DeleteInitiativeServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.commands.CommandsMediatorServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.reward.RewardContextHolderServiceImpl=WARN",
})
class CommandsConsumerConfigTest extends BaseIntegrationTest {
    private final String INITIATIVEID = "INITIATIVEID_%d";
    private final Set<String> USER_INITIATIVES_DISCOUNT = new HashSet<>();
    private final Set<String> INITIATIVES_DELETED_DISCOUNT_OR_SINGLE_REFUND = new HashSet<>();
    private final Set<String> INITIATIVES_DELETED_REFUND = new HashSet<>();

    @SpyBean
    private DroolsRuleRepository droolsRuleRepositorySpy;
    @Autowired
    private HpanInitiativesRepository hpanInitiativesRepository;
    @Autowired
    private TransactionProcessedRepository transactionProcessedRepository;
    @Autowired
    private UserInitiativeCountersRepository userInitiativeCountersRepository;
    @Autowired
    private RewardContextHolderService rewardContextHolderService;
    private static final int VALID_USE_CASES = 4;
    private static final int VALID_MESSAGES = 100;

    @Test
    void test() {
        
        int notValidMessages = errorUseCases.size();
        long maxWaitingMs = 30000;

        List<String> commandsPayloads = new ArrayList<>(notValidMessages+ VALID_MESSAGES);
        commandsPayloads.addAll(IntStream.range(0,notValidMessages).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());
        commandsPayloads.addAll(buildValidPayloads(notValidMessages, VALID_MESSAGES));

        long timeStart=System.currentTimeMillis();
        commandsPayloads.forEach(cp -> kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicCommands, null, null, cp));
        long timePublishingEnd = System.currentTimeMillis();

        waitForLastStorageChange(VALID_MESSAGES /2);
        long timeEnd=System.currentTimeMillis();

        System.out.printf("""
                        ************************
                        Time spent to send %d (%d + %d) messages (from start): %d millis
                        Time spent to assert db stored count (from previous check): %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                commandsPayloads.size(),
                VALID_MESSAGES,
                notValidMessages,
                timePublishingEnd - timeStart,
                timeEnd - timePublishingEnd,
                timeEnd - timeStart
        );

        checkRepositories();
        checkErrorsPublished(notValidMessages, maxWaitingMs, errorUseCases);

        System.out.printf("""
                        ************************
                        Time spent to send %d (%d + %d) messages (from start): %d millis
                        Time spent to assert db stored count (from previous check): %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                commandsPayloads.size(),
                VALID_MESSAGES,
                notValidMessages,
                timePublishingEnd - timeStart,
                timeEnd - timePublishingEnd,
                timeEnd - timeStart
        );
    }

    private long waitForLastStorageChange(int n) {
        long[] countSaved={0};
        //noinspection ConstantConditions
        TestUtils.waitFor(()->(countSaved[0]=transactionProcessedRepository.count().block()) == n, ()->"Expected %d saved users in db, read %d".formatted(n, countSaved[0]), 60, 1000);
        return countSaved[0];
    }

    private Collection<String> buildValidPayloads(int startInterval, int messageNumber) {
        return IntStream.range(startInterval,startInterval+messageNumber)
                .mapToObj(i -> {
                    String initiativeId = INITIATIVEID.formatted(i);
                    CommandOperationDTO command = CommandOperationDTO.builder()
                            .entityId(initiativeId)
                            .operationTime(LocalDateTime.now())
                            .build();
                    switch (i%VALID_USE_CASES){
                        case 0 -> {
                            String userId = initializeDB(i, InitiativeRewardType.DISCOUNT, initiativeId, true);
                            USER_INITIATIVES_DISCOUNT.add(userId);
                            INITIATIVES_DELETED_DISCOUNT_OR_SINGLE_REFUND.add(command.getEntityId());
                            command.setOperationType(CommandsConstants.COMMANDS_OPERATION_TYPE_DELETE_INITIATIVE);
                        }

                        case 1 -> {
                            initializeDB(i, InitiativeRewardType.REFUND, initiativeId, false);
                            INITIATIVES_DELETED_REFUND.add(command.getEntityId());
                            command.setOperationType(CommandsConstants.COMMANDS_OPERATION_TYPE_DELETE_INITIATIVE);
                        }
                        case 2 -> {
                            initializeDB(i, InitiativeRewardType.REFUND, initiativeId, true);
                            INITIATIVES_DELETED_DISCOUNT_OR_SINGLE_REFUND.add(command.getEntityId());
                            command.setOperationType(CommandsConstants.COMMANDS_OPERATION_TYPE_DELETE_INITIATIVE);
                        }
                        default -> {
                            initializeDB(i, InitiativeRewardType.REFUND, initiativeId, true);
                            command.setOperationType("ANOTHER_TYPE");
                        }
                    }
                    return command;
                })
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    private String initializeDB(int bias, InitiativeRewardType initiativeType, String initiativeId, boolean singleInitiative) {
        Reward reward = Reward.builder()
                .initiativeId(initiativeId)
                .providedReward(BigDecimal.TEN)
                .accruedReward(BigDecimal.TEN)
                .build();

        Reward reward2 = Reward.builder()
                .initiativeId("ANOTHER_INITIATIVE")
                .providedReward(BigDecimal.TEN)
                .accruedReward(BigDecimal.TEN)
                .build();

        OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                .initiativeId(initiativeId)
                .activeTimeIntervals(List.of(ActiveTimeInterval.builder()
                        .startInterval(LocalDateTime.now().minusMonths(3L)).build()))
                .build();

        OnboardedInitiative onboardedInitiative2 = OnboardedInitiative.builder()
                .initiativeId("ANOTHER_INITIATIVE")
                .activeTimeIntervals(List.of(ActiveTimeInterval.builder()
                        .startInterval(LocalDateTime.now().minusMonths(3L)).build()))
                .build();

        TransactionProcessed transactionProcessed = TransactionProcessedFaker.mockInstance(bias);
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithoutInitiative(bias);

        if(singleInitiative){
            transactionProcessed.setRewards(Map.of(initiativeId, reward));
            transactionProcessed.setInitiatives(List.of(initiativeId));

            hpanInitiatives.setOnboardedInitiatives(List.of(onboardedInitiative));

        } else {
            transactionProcessed.setRewards(
                    Map.of(initiativeId, reward,
                            "ANOTHER_INITIATIVE", reward2));
            transactionProcessed.setInitiatives(List.of(initiativeId, "ANOTHER_INITIATIVE"));

            hpanInitiatives.setOnboardedInitiatives(List.of(onboardedInitiative, onboardedInitiative2));

        }

        transactionProcessedRepository.save(transactionProcessed).block();

        hpanInitiativesRepository.save(hpanInitiatives).block();

        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .initiativeRewardType(initiativeType)
                .build();

        String INITIATIVENAME = "INITIATIVENAME%d";
        DroolsRule droolsRule = DroolsRule.builder()
                .id(initiativeId)
                .name(INITIATIVENAME.formatted(bias))
                .initiativeConfig(initiativeConfig)
                .rule("")
                .build();

        droolsRuleRepositorySpy.save(droolsRule).block();

        UserInitiativeCounters userInitiativeCounters = UserInitiativeCounters
                .builder(hpanInitiatives.getUserId(), InitiativeGeneralDTO.BeneficiaryTypeEnum.PF, initiativeId)
                .build();

        userInitiativeCountersRepository.save(userInitiativeCounters).block();

        return hpanInitiatives.getUserId();
    }

    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"entityId\":\"ENTITYID_ERROR([0-9]+)\"");
    }

    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();

    {
        String useCaseJsonNotExpected = "{\"entityId\":\"ENTITYID_ERROR0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_CALCULATOR_COMMANDS] Unexpected JSON", useCaseJsonNotExpected)
        ));

        String jsonNotValid = "{\"entityId\":\"ENTITYID_ERROR1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_CALCULATOR_COMMANDS] Unexpected JSON", jsonNotValid)
        ));

        final String errorInitiativeId = "ENTITYID_ERROR2";
        CommandOperationDTO commandOperationError = CommandOperationDTO.builder()
                .entityId(errorInitiativeId)
                .operationType(CommandsConstants.COMMANDS_OPERATION_TYPE_DELETE_INITIATIVE)
                .operationTime(LocalDateTime.now())
                .build();
        String commandOperationErrorString = TestUtils.jsonSerializer(commandOperationError);
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new MongoException("Command error dummy"))
                            .when(droolsRuleRepositorySpy).deleteById(errorInitiativeId);
                    return commandOperationErrorString;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_CALCULATOR_COMMANDS] An error occurred evaluating commands", commandOperationErrorString)
        ));
    }

    private void checkRepositories() {
        Set<String> allInitiativesDeleted = new HashSet<>();
        allInitiativesDeleted.addAll(INITIATIVES_DELETED_DISCOUNT_OR_SINGLE_REFUND);
        allInitiativesDeleted.addAll(INITIATIVES_DELETED_REFUND);

        Assertions.assertTrue(droolsRuleRepository.findAll().toStream().noneMatch(rule -> allInitiativesDeleted.contains(rule.getId())));

        List<HpanInitiatives> hpanInitiativeInDB = hpanInitiativesRepository.findAll().collectList().block();
        Assertions.assertNotNull(hpanInitiativeInDB);
        Assertions.assertEquals(VALID_MESSAGES/2, hpanInitiativeInDB.size());
        hpanInitiativeInDB.forEach(hi ->
                        Assertions.assertTrue(hi.getOnboardedInitiatives().stream().noneMatch(e -> allInitiativesDeleted.contains(e.getInitiativeId())))
        );


        List<BaseTransactionProcessed> trxInDB = transactionProcessedRepository.findAll().collectList().block();
        Assertions.assertNotNull(trxInDB);
        Assertions.assertEquals(VALID_MESSAGES/2, trxInDB.size());
        trxInDB.forEach(trx -> {
            Assertions.assertFalse(USER_INITIATIVES_DISCOUNT.contains(trx.getUserId()));
            Assertions.assertTrue(trx.getInitiatives().stream().noneMatch(allInitiativesDeleted::contains));
        });

        Assertions.assertTrue(userInitiativeCountersRepository.findAll().toStream().noneMatch(us -> allInitiativesDeleted.contains(us.getInitiativeId())));
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(topicCommands, groupIdCommandsConsumer, errorMessage, errorDescription, expectedPayload, null);
    }
}