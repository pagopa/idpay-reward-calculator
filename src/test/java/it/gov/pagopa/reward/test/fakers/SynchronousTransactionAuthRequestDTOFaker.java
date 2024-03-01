package it.gov.pagopa.reward.test.fakers;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionAuthRequestDTO;

public class SynchronousTransactionAuthRequestDTOFaker {

    public static SynchronousTransactionAuthRequestDTO mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    public static SynchronousTransactionAuthRequestDTO.SynchronousTransactionAuthRequestDTOBuilder mockInstanceBuilder(Integer bias) {
        try {
            return TestUtils.objectMapper.readValue(
                            TestUtils.objectMapper.writeValueAsString(SynchronousTransactionRequestDTOFaker.mockInstanceBuilder(bias).build()),
                            SynchronousTransactionAuthRequestDTO.class
                    ).toBuilder()
                    .rewardCents(bias);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot build SynchronousTransactionAuthRequestDTO mockInstance", e);
        }
    }
}
