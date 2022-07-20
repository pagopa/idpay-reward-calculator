package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
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
        HpanInitiatives initiativesForHpan = citizenHpanRepository.findById(hpan).block();
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

    boolean checkDate2(LocalDateTime trxDate, List<ActiveTimeInterval> timeIntervals){
        for(int i=timeIntervals.size()-1; i==0; i--){
            LocalDateTime start = timeIntervals.get(i).getStartInterval();
            LocalDateTime end = timeIntervals.get(i).getEndInterval();
            if ((trxDate.isAfter(start) || trxDate.isEqual(start)) && (end == null || trxDate.isBefore(end))) {
                return true;
            }
        }
        return false;
    }


}
