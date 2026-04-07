package it.gov.pagopa.reward.test.fakers;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionAuthRequestDTO;
import tools.jackson.core.JacksonException;

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
        } catch (JacksonException e) {
            throw new IllegalStateException("Cannot build SynchronousTransactionAuthRequestDTO mockInstance", e);
        }
    }
}
