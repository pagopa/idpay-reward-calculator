package it.gov.pagopa;

import it.gov.pagopa.dto.TransactionDTO;
import it.gov.pagopa.dto.RewardTransactionDTO;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(SpringExtension.class)
class TransactionTest {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionTest.class);
    private static final String STRING_NULL = "Null";
    private static final String BASE_URL = "http://localhost:8080/idpay/transactions";

    @SneakyThrows
    @Test
    void TransactionPocTest() {

        RestTemplate restTemplate = new RestTemplate();
        DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory();
        uriFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
        restTemplate.setUriTemplateHandler(uriFactory);

        try (BufferedReader br = new BufferedReader(new FileReader(new ClassPathResource("input20.csv").getFile()))) {
            String line = br.readLine();
            while (line != null) {
                TransactionDTO dto = TransactionFactory.createTransactions(line);
                Instant before, after;

                before = Instant.now();
                restTemplate.postForEntity(BASE_URL, dto, Void.class);
                after = Instant.now();
                LOG.info("Time between before and after send messages to event-hub: " + Duration.between(before, after).toMillis() + " ms");

                line = br.readLine();
            }
        } catch (IOException e) {
            Assertions.fail();
        }

        try {
            FileReader fileReaderResults = new FileReader(new ClassPathResource("output20.csv").getFile());
            BufferedReader brResults = new BufferedReader(fileReaderResults);
            String lineResults = brResults.readLine();
            while (lineResults != null) {
                String[] resultsField = lineResults.split(";");

                Map<String, String> uriVariables = new HashMap<>();
                uriVariables.put("idTrxAcquirer", resultsField[0]);
                uriVariables.put("acquirerCode", resultsField[1]);
                uriVariables.put("trxDate", resultsField[2]);

                //read attended results
                ResponseEntity<RewardTransactionDTO> resp = restTemplate
                        .getForEntity("http://localhost:8080/idpay/transactions?idTrxAcquirer={idTrxAcquirer}&acquirerCode={acquirerCode}&trxDate={trxDate}",
                                RewardTransactionDTO.class,
                                uriVariables);

                String expected = resultsField[3].equals(STRING_NULL) ? STRING_NULL : resultsField[3];
                String actual;
                if (resp.getBody() != null) {
                    actual = resp.getBody().getReward() == null ? STRING_NULL : resp.getBody().getReward().toString();
                    Assertions.assertEquals(expected, actual);
                    LOG.info("Expected value: " + expected + ", actual Value: " + actual);
                }
                lineResults = brResults.readLine();
            }
        } catch (IOException e) {
            Assertions.fail();
        }
    }

    static class TransactionFactory {
        public static TransactionDTO createTransactions(String line) {
            TransactionDTO ret = null;
            try {
                String[] trx = line.split(";");
                if (trx.length > 14) {
                    ret = TransactionDTO.builder()
                            .acquirerCode(trx[0])
                            .operationType(trx[1])
                            .circuitType(trx[2])
                            .hpan(trx[3])
                            .trxDate(trx[4])
                            .idTrxAcquirer(trx[5])
                            .idTrxIssuer(trx[6])
                            .correlationId(trx[7])
                            .amount(new BigDecimal(trx[8]))
                            .amountCurrency(trx[9])
                            .acquirerId(trx[10])
                            .merchantId(trx[11])
                            .terminalId(trx[12])
                            .bin(trx[13])
                            .mcc(trx[14])
                            .build();
                }
            } catch (Exception e) {
                ret = null;
            }
            return ret;
        }
    }
}
