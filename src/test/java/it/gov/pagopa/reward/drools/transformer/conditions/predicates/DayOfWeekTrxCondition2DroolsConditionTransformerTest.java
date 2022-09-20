package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.reward.dto.rule.trx.DayOfWeekDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

class DayOfWeekTrxCondition2DroolsConditionTransformerTest extends InitiativeTrxCondition2DroolsConditionTransformerTest {

    private final DayOfWeekTrxCondition2DroolsConditionTransformer transformer = new DayOfWeekTrxCondition2DroolsConditionTransformer();
    
    private final String initiativeId = "DayOfWeek";

    @Test
    void testNoDayConfigured() {
        
        DayOfWeekDTO dayOfWeekDTO = new DayOfWeekDTO();

        String dayOfWeekCondition = transformer.apply(initiativeId, dayOfWeekDTO);

        Assertions.assertEquals("false", dayOfWeekCondition);

        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        trx.setTrxDate(OffsetDateTime.now());
        testRule(initiativeId, dayOfWeekCondition, trx, false);

        dayOfWeekDTO= new DayOfWeekDTO();
        String dayOfWeekConditionEmptyCollection = transformer.apply(initiativeId, dayOfWeekDTO);

        Assertions.assertEquals("false", dayOfWeekConditionEmptyCollection);

        testRule(initiativeId, dayOfWeekConditionEmptyCollection, trx, false);
    }

    //region no interval
    private String buildNoIntervalDto() {
        DayOfWeekDTO dayOfWeekDTO = new DayOfWeekDTO(List.of(
                DayOfWeekDTO.DayConfig.builder()
                        .daysOfWeek(new TreeSet<>(Set.of(
                                DayOfWeek.SATURDAY,
                                DayOfWeek.MONDAY
                        )))
                        .build(),
                DayOfWeekDTO.DayConfig.builder()
                        .daysOfWeek(new TreeSet<>(Set.of(
                                DayOfWeek.SUNDAY,
                                DayOfWeek.THURSDAY
                        )))
                        .build()
        ));

        String dayOfWeekCondition = transformer.apply(initiativeId, dayOfWeekDTO);

        Assertions.assertEquals("(" +
                        "(trxDate.dayOfWeek in (java.time.DayOfWeek.valueOf(\"MONDAY\"),java.time.DayOfWeek.valueOf(\"SATURDAY\")))" +
                        " || " +
                        "(trxDate.dayOfWeek in (java.time.DayOfWeek.valueOf(\"THURSDAY\"),java.time.DayOfWeek.valueOf(\"SUNDAY\")))" +
                        ")"
                , dayOfWeekCondition);

        return dayOfWeekCondition;
    }

    @Test
    void testNoIntervalExpectingTrue() {
        String dayOfWeekCondition = buildNoIntervalDto();

        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(0, 0));
        trx.setTrxDate(OffsetDateTime.of(localDateTime, RewardConstants.ZONEID.getRules().getOffset(localDateTime)));
        testRule(initiativeId, dayOfWeekCondition, trx, true);
    }

    @Test
    void testNoIntervalExpectingFalse() {
        String dayOfWeekCondition = buildNoIntervalDto();

        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2022, 1, 7), LocalTime.of(0, 0));
        trx.setTrxDate(OffsetDateTime.of(localDateTime, RewardConstants.ZONEID.getRules().getOffset(localDateTime)));
        testRule(initiativeId, dayOfWeekCondition, trx, false);
    }
    //endregion

    //region complete setting
    private String buildCompleteDto() {
        DayOfWeekDTO dayOfWeekDTO = new DayOfWeekDTO(List.of(
                DayOfWeekDTO.DayConfig.builder()
                        .daysOfWeek(new TreeSet<>(Set.of(
                                DayOfWeek.SATURDAY,
                                DayOfWeek.MONDAY
                        )))
                        .intervals(List.of(
                                DayOfWeekDTO.Interval.builder()
                                        .startTime(LocalTime.of(3, 23))
                                        .endTime(LocalTime.of(5, 37))
                                        .build(),
                                DayOfWeekDTO.Interval.builder()
                                        .startTime(LocalTime.of(12, 0))
                                        .endTime(LocalTime.of(22, 15))
                                        .build()
                        ))
                        .build(),
                DayOfWeekDTO.DayConfig.builder()
                        .daysOfWeek(new TreeSet<>(Set.of(
                                DayOfWeek.SUNDAY,
                                DayOfWeek.THURSDAY
                        )))
                        .intervals(List.of(
                                DayOfWeekDTO.Interval.builder()
                                        .startTime(LocalTime.of(0, 0))
                                        .endTime(LocalTime.of(23, 59, 59, 999000000))
                                        .build()
                        ))
                        .build()
        ));

        String dayOfWeekCondition = transformer.apply(initiativeId, dayOfWeekDTO);

        Assertions.assertEquals("(" +
                        "("
                        + "trxDate.dayOfWeek in (java.time.DayOfWeek.valueOf(\"MONDAY\"),java.time.DayOfWeek.valueOf(\"SATURDAY\"))"
                        + " && "
                        + "("
                        +    "(trxDate.atZoneSameInstant(java.time.ZoneId.of(\"Europe/Rome\")).toLocalTime() >= java.time.LocalTime.of(3,23,0,0) && trxDate.atZoneSameInstant(java.time.ZoneId.of(\"Europe/Rome\")).toLocalTime() <= java.time.LocalTime.of(5,37,0,0))"
                        +    " || "
                        +    "(trxDate.atZoneSameInstant(java.time.ZoneId.of(\"Europe/Rome\")).toLocalTime() >= java.time.LocalTime.of(12,0,0,0) && trxDate.atZoneSameInstant(java.time.ZoneId.of(\"Europe/Rome\")).toLocalTime() <= java.time.LocalTime.of(22,15,0,0))"
                        + ")" +
                        ") || " +
                        "("
                        + "trxDate.dayOfWeek in (java.time.DayOfWeek.valueOf(\"THURSDAY\"),java.time.DayOfWeek.valueOf(\"SUNDAY\"))"
                        + " && "
                        + "((trxDate.atZoneSameInstant(java.time.ZoneId.of(\"Europe/Rome\")).toLocalTime() >= java.time.LocalTime.of(0,0,0,0) && trxDate.atZoneSameInstant(java.time.ZoneId.of(\"Europe/Rome\")).toLocalTime() <= java.time.LocalTime.of(23,59,59,999000000)))" +
                        ")" +
                        ")"
                , dayOfWeekCondition);

        return dayOfWeekCondition;
    }

    @Test
    void testCompleteSettingExpectingTrue() {
        String dayOfWeekCondition = buildCompleteDto();

        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(12, 0));
        trx.setTrxDate(OffsetDateTime.of(localDateTime, RewardConstants.ZONEID.getRules().getOffset(localDateTime)));
        testRule(initiativeId, dayOfWeekCondition, trx, true);
    }

    @Test
    void testCompleteSettingExpectingFalse() {
        String dayOfWeekCondition = buildCompleteDto();

        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(11, 59));
        trx.setTrxDate(OffsetDateTime.of(localDateTime, RewardConstants.ZONEID.getRules().getOffset(localDateTime)));
        testRule(initiativeId, dayOfWeekCondition, trx, false);
    }
    //endregion
}
