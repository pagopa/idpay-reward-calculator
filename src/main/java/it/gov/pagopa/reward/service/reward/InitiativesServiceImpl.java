package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.CitizenHpan;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.repository.CitizenHpanRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class InitiativesServiceImpl implements InitiativesService{

    private final CitizenHpanRepository citizenHpanRepository;

    public InitiativesServiceImpl(CitizenHpanRepository citizenHpanRepository){
        this.citizenHpanRepository = citizenHpanRepository;
    }

    @Override
    public List<String> getInitiatives(String hpan, OffsetDateTime trxDate) {
        CitizenHpan initiativesForHpan = citizenHpanRepository.findById(hpan).block();
        LocalDateTime trxDateTime = trxDate.atZoneSameInstant(ZoneId.of("Europe/Rome")).toLocalDateTime();
        List<String> initiatives = new ArrayList<>();

        if(initiativesForHpan!=null && initiativesForHpan.getOnboardedInitiatives()!=null){
            List<OnboardedInitiative> onboardedInitiatives = initiativesForHpan.getOnboardedInitiatives();
            for (OnboardedInitiative i : onboardedInitiatives) {
                List<ActiveTimeInterval> timeActiveInitiative = i.getActiveTimeIntervals();
                for (ActiveTimeInterval p : timeActiveInitiative) {
                    LocalDateTime start = p.getStartInterval();
                    LocalDateTime end = p.getEndInterval();
                    if ((trxDateTime.isAfter(start) || trxDateTime.isEqual(start)) && (end == null || trxDateTime.isBefore(end))) {
                        initiatives.add(i.getInitiativeId());
                    }
                }
            }
        }
        return initiatives;
    }
}
