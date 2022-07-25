package it.gov.pagopa.reward.dto.rule.trx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class InitiativeTrxConditionsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public InitiativeTrxConditionsTest() {
        objectMapper.registerModule(new JavaTimeModule()); // TODO check if Spring add it
    }


    private void testDeserialization(String content, InitiativeTrxConditions expected) throws JsonProcessingException {
        Assertions.assertEquals(expected, objectMapper.readValue(content, InitiativeTrxConditions.class));
        Assertions.assertEquals(content.trim(), objectMapper.writeValueAsString(expected));
    }

    @Test
    public void test() throws JsonProcessingException {
        String content = """
                {"daysAllowed":%s,"threshold":%s,"mccFilter":%s,"trxCount":%s,"rewardLimits":%s}
                """.formatted(
                "{\"dayOfWeeks\":[\"MONDAY\",\"THURSDAY\"],\"interval\":{\"startTime\":[6,0],\"endTime\":[22,0,59,999000000]}}",
                "{\"from\":10.00,\"fromIncluded\":true,\"to\":12.32,\"toIncluded\":false}",
                "{\"allowedList\":true,\"values\":[\"1200\",\"2223\",\"4455\"]}",
                "{\"from\":2,\"fromIncluded\":true,\"to\":5,\"toIncluded\":false}",
                "[{\"frequency\":\"DAILY\",\"rewardLimit\":70.00},{\"frequency\":\"MONTHLY\",\"rewardLimit\":700.00}]"
        );

        InitiativeTrxConditions expected = InitiativeTrxConditions.builder()
                .daysAllowed(DayOfWeekDTO.builder() // TODO more flexible
                        .dayOfWeeks(new TreeSet<>(Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)))
                        .interval(DayOfWeekDTO.IntervalDTO.builder()
                                .startTime(LocalTime.of(6, 0))
                                .endTime(LocalTime.of(22, 0, 59, 999000000))
                                .build())
                        .build()
                )
                .threshold(ThresholdDTO.builder()
                        .from(BigDecimal.valueOf(10).setScale(2, RoundingMode.UNNECESSARY))
                        .fromIncluded(true)
                        .to(BigDecimal.valueOf(12.32).setScale(2, RoundingMode.UNNECESSARY))
                        .build())
                .mccFilter(MccFilterDTO.builder()
                        .allowedList(true)
                        .values(new TreeSet<>(Set.of("1200", "2223", "4455")))
                        .build())
                .rewardLimits(List.of(
                        RewardLimitsDTO.builder()
                                .frequency(RewardLimitsDTO.RewardLimitFrequency.DAILY)
                                .rewardLimit(BigDecimal.valueOf(70.00).setScale(2, RoundingMode.UNNECESSARY))
                                .build(),
                        RewardLimitsDTO.builder()
                                .frequency(RewardLimitsDTO.RewardLimitFrequency.MONTHLY)
                                .rewardLimit(BigDecimal.valueOf(700.00).setScale(2, RoundingMode.UNNECESSARY))
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
