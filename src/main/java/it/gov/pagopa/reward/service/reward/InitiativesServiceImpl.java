package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class InitiativesServiceImpl implements InitiativesService{

    private final HpanInitiativesRepository hpanInitiativesRepository;

    public InitiativesServiceImpl(HpanInitiativesRepository hpanInitiativesRepository){
        this.hpanInitiativesRepository = hpanInitiativesRepository;
    }

    @Override
    public List<String> getInitiatives(String hpan, OffsetDateTime trxDate) {
        HpanInitiatives initiativesForHpan = hpanInitiativesRepository.findById(hpan).block();
        LocalDateTime trxDateTime = trxDate.atZoneSameInstant(ZoneId.of("Europe/Rome")).toLocalDateTime();
        List<String> initiatives = new ArrayList<>();

        if(initiativesForHpan!=null && initiativesForHpan.getOnboardedInitiatives()!=null){
            List<OnboardedInitiative> onboardedInitiatives = initiativesForHpan.getOnboardedInitiatives();
            for (OnboardedInitiative i : onboardedInitiatives) {
                if(checkDate(trxDateTime, i.getActiveTimeIntervals())){
                    initiatives.add(i.getInitiativeId());
                }
            }
        }
        return initiatives;
    }

    boolean checkDate(LocalDateTime trxDate, List<ActiveTimeInterval> timeIntervals){
        for (ActiveTimeInterval p : timeIntervals) {
            LocalDateTime start = p.getStartInterval();
            LocalDateTime end = p.getEndInterval();
            if ((trxDate.isAfter(start) || trxDate.isEqual(start)) && (end == null || trxDate.isBefore(end))) {
                return true;
            }
        }
        return false;
    }
}
