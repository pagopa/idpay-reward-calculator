package it.gov.pagopa.reward.dto.rule.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class InitiativeRewardRuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private void testDeserialization(String content, InitiativeRewardRule expected) throws JsonProcessingException {
        Assertions.assertEquals(expected, objectMapper.readValue(content, InitiativeRewardRule.class));
        Assertions.assertEquals(content.trim(), objectMapper.writeValueAsString(expected));
    }

    @Test
    public void testRewardGroups() throws JsonProcessingException {
        String content = """
                {"_type":"rewardGroups","rewardGroups":[{"from":0.00,"to":2.00,"rewardValue":10},{"from":3.00,"to":4.55,"rewardValue":12.5}]}
                """;

        RewardGroupsDTO expected = RewardGroupsDTO.builder()
                .rewardGroups(List.of(
                        RewardGroupsDTO.RewardGroupDTO.builder()
                                .from(BigDecimal.valueOf(0).setScale(2, RoundingMode.UNNECESSARY))
                                .to(BigDecimal.valueOf(2).setScale(2, RoundingMode.UNNECESSARY))
                                .rewardValue(BigDecimal.valueOf(10))
                                .build(),
                        RewardGroupsDTO.RewardGroupDTO.builder()
                                .from(BigDecimal.valueOf(3).setScale(2, RoundingMode.UNNECESSARY))
                                .to(BigDecimal.valueOf(4.55))
                                .rewardValue(BigDecimal.valueOf(12.5))
                                .build()
                ))
                .build();

        testDeserialization(content, expected);
    }

    @Test
    public void testRewardValue() throws JsonProcessingException {
        String content = """
                {"_type":"rewardValue","rewardValue":10.00}
                """;

        RewardValueDTO expected = RewardValueDTO.builder()
                .rewardValue(BigDecimal.valueOf(10).setScale(2, RoundingMode.UNNECESSARY))
                .build();

        testDeserialization(content, expected);
    }
}
