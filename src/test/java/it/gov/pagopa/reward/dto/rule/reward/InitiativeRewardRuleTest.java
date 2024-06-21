package it.gov.pagopa.reward.dto.rule.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.reward.enums.RewardValueType;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

class InitiativeRewardRuleTest {

    private void testDeserialization(String content, InitiativeRewardRule expected) throws JsonProcessingException {
        content = content.replaceAll("(\"rewardValue\":[0-9.]+)}", "$1,\"rewardValueType\":\"PERCENTAGE\"}");
        Assertions.assertEquals(expected, TestUtils.objectMapper.readValue(content, InitiativeRewardRule.class));
        Assertions.assertEquals(content.trim(), TestUtils.objectMapper.writeValueAsString(expected));
    }

    @Test
    void testRewardGroups() throws JsonProcessingException {
        String content = """
                {"_type":"rewardGroups","rewardGroups":[{"fromCents":0,"toCents":200,"rewardValue":10},{"fromCents":300,"toCents":455,"rewardValue":12.5,"rewardValueType":"PERCENTAGE"},{"fromCents":500,"toCents":789,"rewardValue":350,"rewardValueType":"ABSOLUTE"}]}
                """;

        RewardGroupsDTO expected = RewardGroupsDTO.builder()
                .rewardGroups(List.of(
                        RewardGroupsDTO.RewardGroupDTO.builder()
                                .fromCents(0L)
                                .toCents(2_00L)
                                .rewardValue(BigDecimal.valueOf(10))
                                .rewardValueType(RewardValueType.PERCENTAGE)
                                .build(),
                        RewardGroupsDTO.RewardGroupDTO.builder()
                                .fromCents(3_00L)
                                .toCents(4_55L)
                                .rewardValue(BigDecimal.valueOf(12.5))
                                .rewardValueType(RewardValueType.PERCENTAGE)
                                .build(),
                        RewardGroupsDTO.RewardGroupDTO.builder()
                                .fromCents(5_00L)
                                .toCents(7_89L)
                                .rewardValue(BigDecimal.valueOf(350))
                                .rewardValueType(RewardValueType.ABSOLUTE)
                                .build()
                ))
                .build();

        testDeserialization(content, expected);
    }

    @Test
    void testRewardValue() throws JsonProcessingException {
        String content = """
                {"_type":"rewardValue","rewardValue":10.00}
                """;

        RewardValueDTO expected = RewardValueDTO.builder()
                .rewardValue(BigDecimal.valueOf(10).setScale(2, RoundingMode.UNNECESSARY))
                .build();

        testDeserialization(content, expected);
    }
}
