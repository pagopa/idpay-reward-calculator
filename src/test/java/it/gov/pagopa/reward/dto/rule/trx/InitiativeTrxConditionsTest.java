package it.gov.pagopa.reward.dto.rule.trx;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

class InitiativeTrxConditionsTest {

    private void testDeserialization(String content, InitiativeTrxConditions expected) throws JsonProcessingException {
        Assertions.assertEquals(expected, TestUtils.objectMapper.readValue(content, InitiativeTrxConditions.class));
        Assertions.assertEquals(content.trim(), TestUtils.objectMapper.writeValueAsString(expected));
    }

    @Test
    void test() throws JsonProcessingException {
        String content = """
                {"daysOfWeek":%s,"threshold":%s,"mccFilter":%s,"trxCount":%s,"rewardLimits":%s}
                """.formatted(
                "[" +
                        "{\"daysOfWeek\":[\"MONDAY\",\"SUNDAY\"],\"intervals\":[{\"startTime\":\"06:00:00.000\",\"endTime\":\"22:00:59.999\"}]}," +
                        "{\"daysOfWeek\":[\"THURSDAY\"],\"intervals\":[{\"startTime\":\"05:00:00.000\",\"endTime\":\"12:59:59.999\"},{\"startTime\":\"18:00:00.000\",\"endTime\":\"23:59:59.999\"}]}" +
                        "]",
                "{\"fromCents\":1000,\"fromIncluded\":true,\"toCents\":1232,\"toIncluded\":false}",
                "{\"allowedList\":true,\"values\":[\"1200\",\"2223\",\"4455\"]}",
                "{\"from\":2,\"fromIncluded\":true,\"to\":5,\"toIncluded\":false}",
                "[{\"frequency\":\"DAILY\",\"rewardLimitCents\":7000},{\"frequency\":\"MONTHLY\",\"rewardLimitCents\":70000}]"
        );

        InitiativeTrxConditions expected = InitiativeTrxConditions.builder()
                .daysOfWeek(new DayOfWeekDTO(List.of(
                                DayOfWeekDTO.DayConfig.builder()
                                        .daysOfWeek(new TreeSet<>(Set.of(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)))
                                        .intervals(List.of(
                                                DayOfWeekDTO.Interval.builder()
                                                        .startTime(LocalTime.of(6, 0))
                                                        .endTime(LocalTime.of(22, 0, 59, 999000000))
                                                        .build()))
                                        .build(),
                                DayOfWeekDTO.DayConfig.builder()
                                        .daysOfWeek(new TreeSet<>(Set.of(DayOfWeek.THURSDAY)))
                                        .intervals(List.of(
                                                DayOfWeekDTO.Interval.builder()
                                                        .startTime(LocalTime.of(5, 0))
                                                        .endTime(LocalTime.of(12, 59, 59, 999000000))
                                                        .build(),
                                                DayOfWeekDTO.Interval.builder()
                                                        .startTime(LocalTime.of(18, 0))
                                                        .endTime(LocalTime.of(23, 59, 59, 999000000))
                                                        .build()))
                                        .build()
                        ))
                )
                .threshold(ThresholdDTO.builder()
                        .fromCents(10_00L)
                        .fromIncluded(true)
                        .toCents(12_32L)
                        .build())
                .mccFilter(MccFilterDTO.builder()
                        .allowedList(true)
                        .values(new TreeSet<>(Set.of("1200", "2223", "4455")))
                        .build())
                .rewardLimits(List.of(
                        RewardLimitsDTO.builder()
                                .frequency(RewardLimitsDTO.RewardLimitFrequency.DAILY)
                                .rewardLimitCents(70_00L)
                                .build(),
                        RewardLimitsDTO.builder()
                                .frequency(RewardLimitsDTO.RewardLimitFrequency.MONTHLY)
                                .rewardLimitCents(700_00L)
                                .build()
                ))
                .trxCount(TrxCountDTO.builder()
                        .from(2L)
                        .fromIncluded(true)
                        .to(5L)
                        .build())
                .build();

        testDeserialization(content, expected);
    }
}
