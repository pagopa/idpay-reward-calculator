package it.gov.pagopa.service.reward;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class InitiativesServiceImpl implements InitiativesService{
    @Override
    public List<String> getInitiatives(String hpan, OffsetDateTime trxDate) {
        List<String> initiative = Arrays.asList("ini001","ini002","ini003","ini004");
        return initiative.subList(1,3);
    }
}
