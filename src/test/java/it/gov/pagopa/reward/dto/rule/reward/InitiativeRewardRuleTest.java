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
                {"_type":"rewardGroups","rewardGroups":[{"from":0.00,"to":2.00,"rewardValue":10},{"from":3.00,"to":4.55,"rewardValue":12.5,"rewardValueType":"PERCENTAGE"},{"from":5.00,"to":7.89,"rewardValue":3.5,"rewardValueType":"ABSOLUTE"}]}
                """;

        RewardGroupsDTO expected = RewardGroupsDTO.builder()
                .rewardGroups(List.of(
                        RewardGroupsDTO.RewardGroupDTO.builder()
                                .from(BigDecimal.valueOf(0).setScale(2, RoundingMode.UNNECESSARY))
                                .to(BigDecimal.valueOf(2).setScale(2, RoundingMode.UNNECESSARY))
                                .rewardValue(BigDecimal.valueOf(10))
                                .rewardValueType(RewardValueType.PERCENTAGE)
                                .build(),
                        RewardGroupsDTO.RewardGroupDTO.builder()
                                .from(BigDecimal.valueOf(3).setScale(2, RoundingMode.UNNECESSARY))
                                .to(BigDecimal.valueOf(4.55))
                                .rewardValue(BigDecimal.valueOf(12.5))
                                .rewardValueType(RewardValueType.PERCENTAGE)
                                .build(),
                        RewardGroupsDTO.RewardGroupDTO.builder()
                                .from(BigDecimal.valueOf(5).setScale(2, RoundingMode.UNNECESSARY))
                                .to(BigDecimal.valueOf(7.89))
                                .rewardValue(BigDecimal.valueOf(3.5))
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
