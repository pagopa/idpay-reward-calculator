package it.gov.pagopa.reward.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Map;

@Slf4j
class TransactionProcessorTest extends BaseIntegrationTest {

    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected HpanInitiativesRepository hpanInitiativesRepository;
    @Autowired
    protected DroolsRuleRepository droolsRuleRepository;
    @Autowired
    protected UserInitiativeCountersRepository userInitiativeCountersRepository;

    @Test
    void testTrxProcessor() throws JsonProcessingException {
        // Given
        Mono<HpanInitiatives> hpanInitiativesMono = hpanInitiativesRepository.findById("5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0");
        log.info(hpanInitiativesMono.toString());
        //region Repository initializer
        ActiveTimeInterval interval1 = ActiveTimeInterval.builder()
                .startInterval(LocalDateTime.now()).endInterval(LocalDateTime.now().plusDays(1L)).build();

        ActiveTimeInterval interval2 = ActiveTimeInterval.builder()
                .startInterval(LocalDateTime.now().plusDays(2L)).build();

        OnboardedInitiative onboardedInitiative1 = OnboardedInitiative.builder()
                .initiativeId("initiativeId1")
                .acceptanceDate(LocalDate.now())
                .status("ACCEPTED")
                .activeTimeIntervals(new ArrayList<>()).build();
        onboardedInitiative1.getActiveTimeIntervals().add(interval1);
        onboardedInitiative1.getActiveTimeIntervals().add(interval2);

        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .hpan("5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0")
                .userId("5c6bda1b1f5f6238dcba")
                .onboardedInitiatives(new ArrayList<>()).build();
        hpanInitiatives.getOnboardedInitiatives().add(onboardedInitiative1);
        hpanInitiativesRepository.save(hpanInitiatives).subscribe(i -> log.info(i.toString()));
        //endregion

        //region Save rule in DB
        DroolsRule droolsRule = DroolsRule.builder()
                .id("initiativeId1")
                .name("initiativeId1")
                .rule("""
                        package it.gov.pagopa.reward.drools.buildrules;
                        import it.gov.pagopa.reward.model.TransactionDroolsDTO;
                        rule "initiativeId1"
                        agenda-group "initiativeId1"
                        when $trx: RewardTransaction(hpan=="5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0")
                        then System.out.println($trx.getRewards());
                        end
                        """)
                .initiativeConfig(new InitiativeConfig())
                .build();

        //endregion
        droolsRuleRepository.save(droolsRule).block();


        //region TransactionDTO initializer
        TransactionDTO trx = TransactionDTO.builder()
                .idTrxAcquirer("98174002165501220007165503")
                .acquirerCode("36081")
                .trxDate(OffsetDateTime.parse("2022-09-07T15:58:42.000+00:00"))
                .hpan("5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0")
                .operationType("00")
                .amount(new BigDecimal("200.00"))
                .acquirerId("09509")
                .mcc("4040")
                .build();
        //endregion

        publishIntoEmbeddedKafka(topicRewardProcessorRequest,null,null,trx);

        //Then
        Consumer<String, String> consumer = getEmbeddedKafkaConsumer(topicRewardProcessorOutcome, "test_groupid");

        for(int i=0, j=0; i<=getExpectedPublishedMessagesCount() && j<=getExpectedPublishedMessagesCount(); j++){
            ConsumerRecords<String,String> publish = consumer.poll(Duration.ofMillis(7000));
            for (ConsumerRecord<String, String> record: publish) {
                RewardTransactionDTO trxOut = objectMapper.readValue(record.value(),RewardTransactionDTO.class);
                Assertions.assertNotNull(trxOut.getRewards());
                Assertions.assertTrue(trxOut.getRewards().isEmpty());
                Assertions.assertNotNull(trxOut.getInitiatives());
                Assertions.assertTrue(trxOut.getInitiatives().contains("initiativeId1"));
            }
        }
/* TODO rewrite the entire test! it's not working
        UserInitiativeCounters userCounters = userInitiativeCountersRepository.findById(trx.getHpan()).block(); // TODO use userId
        Assertions.assertNotNull(userCounters);
        Assertions.assertEquals(trx.getHpan(), userCounters.getUserId());
        Assertions.assertEquals(
                Map.of("initiativeId1",
                        InitiativeCounters.builder()
                                .initiativeId("initiativeId1")
                                .trxNumber(1L)
                                .totalReward(BigDecimal.ZERO) // TODO fix this test: it's not using real rules
                                .totalAmount(trx.getAmount())
                                .build()),
                userCounters.getInitiatives()
        );*/
    }

    int getExpectedPublishedMessagesCount() {
        return 1;
    }



}






