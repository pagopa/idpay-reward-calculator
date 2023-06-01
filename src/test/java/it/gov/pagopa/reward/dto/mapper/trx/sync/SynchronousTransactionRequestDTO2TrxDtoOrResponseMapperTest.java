package it.gov.pagopa.reward.dto.mapper.trx.sync;


import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionRequestDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

public class SynchronousTransactionRequestDTO2TrxDtoOrResponseMapperTest {

    @Test
    void applyChargeOperationTest(){
        // Give
        String chargeOperation = "00";

        SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper mapper = new SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper(chargeOperation);
        SynchronousTransactionRequestDTO previewRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        BigDecimal expectedAmountEur = new BigDecimal("10.00");
        // When
        TransactionDTO result = mapper.apply(previewRequest);

        // Then
        commonOperationTypeAssertions(previewRequest, result, expectedAmountEur);
        Assertions.assertEquals(chargeOperation, result.getOperationType());
    }

    private void commonOperationTypeAssertions(SynchronousTransactionRequestDTO previewRequest, TransactionDTO result, BigDecimal expectedAmountEur) {
        Assertions.assertNotNull(result);
        Assertions.assertEquals(previewRequest.getTransactionId(), result.getId());
        Assertions.assertEquals(previewRequest.getIdTrxAcquirer(), result.getIdTrxAcquirer());
        Assertions.assertEquals(previewRequest.getAcquirerCode(), result.getAcquirerCode());
        Assertions.assertEquals(previewRequest.getTrxDate(), result.getTrxDate());
        Assertions.assertEquals(SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper.getPaymentInstrument(previewRequest.getUserId()), result.getHpan());
        Assertions.assertEquals(previewRequest.getIdTrxIssuer(), result.getIdTrxIssuer());
        Assertions.assertEquals(expectedAmountEur, result.getAmount());
        Assertions.assertEquals(previewRequest.getAmountCurrency(), result.getAmountCurrency());
        Assertions.assertEquals(previewRequest.getMcc(), result.getMcc());
        Assertions.assertEquals(previewRequest.getAcquirerId(), result.getAcquirerId());
        Assertions.assertEquals(previewRequest.getMerchantId(), result.getMerchantId());
        Assertions.assertEquals(previewRequest.getMerchantFiscalCode(), result.getFiscalCode());
        Assertions.assertEquals(previewRequest.getVat(), result.getVat());
        Assertions.assertEquals(previewRequest.getUserId(), result.getUserId());
        Assertions.assertEquals(previewRequest.getAmountCents(), result.getAmountCents());
        Assertions.assertEquals(expectedAmountEur, result.getEffectiveAmount());
        Assertions.assertEquals(previewRequest.getTransactionId(), result.getCorrelationId());
        Assertions.assertEquals(previewRequest.getTrxChargeDate(),result.getTrxChargeDate());
        Assertions.assertEquals(previewRequest.getChannel(),result.getChannel());

        TestUtils.checkNotNullFields(result, "circuitType", "terminalId", "bin", "senderCode",
                "posType","par", "refundInfo", "brandLogo", "brand", "maskedPan", "ruleEngineTopicPartition", "ruleEngineTopicOffset");
    }

    @Test
    void apply2SynchronousResponseTest(){
        // Give
        String chargeOperation = "00";
        SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper mapper = new SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper(chargeOperation);

        SynchronousTransactionRequestDTO previewRequest = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        String initiativeId="INITIATIVEID";
        List<String> discardCause = List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE);
        // When
        SynchronousTransactionResponseDTO result = mapper.apply(previewRequest,initiativeId,discardCause);

        // Then
        Assertions.assertNotNull(result);
        errorResponseCommonAssertions(previewRequest, initiativeId, discardCause, result);
    }

    public static void errorResponseCommonAssertions(SynchronousTransactionRequestDTO previewRequest, String initiativeId, List<String> discardCause, SynchronousTransactionResponseDTO result) {
        Assertions.assertEquals(previewRequest.getTransactionId(), result.getTransactionId());
        Assertions.assertEquals(initiativeId, result.getInitiativeId());
        Assertions.assertEquals(previewRequest.getUserId(), result.getUserId());
        Assertions.assertEquals(previewRequest.getAmountCents(), result.getAmountCents());
        Assertions.assertEquals(CommonUtilities.centsToEuro(previewRequest.getAmountCents()), result.getAmount());
        Assertions.assertEquals(CommonUtilities.centsToEuro(previewRequest.getAmountCents()), result.getEffectiveAmount());
        Assertions.assertEquals(RewardConstants.REWARD_STATE_REJECTED, result.getStatus());
        Assertions.assertEquals(discardCause, result.getRejectionReasons());
        Assertions.assertNull(result.getReward());
        TestUtils.checkNotNullFields(result, "reward");
    }
}