package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OnboardedInitiativesServiceImpl implements OnboardedInitiativesService {

    private final HpanInitiativesRepository hpanInitiativesRepository;

    public OnboardedInitiativesServiceImpl(HpanInitiativesRepository hpanInitiativesRepository){
        this.hpanInitiativesRepository = hpanInitiativesRepository;
    }

    @Override
    public Flux<String> getInitiatives(String hpan, OffsetDateTime trxDate) {
        return hpanInitiativesRepository.findById(hpan)
                .flatMapMany(initiativesForHpan -> {
                    LocalDateTime trxDateTime = trxDate.atZoneSameInstant(ZoneId.of("Europe/Rome")).toLocalDateTime();
                    List<String> initiatives = new ArrayList<>();

                    if (initiativesForHpan != null && initiativesForHpan.getOnboardedInitiatives() != null) {
                        List<OnboardedInitiative> onboardedInitiatives = initiativesForHpan.getOnboardedInitiatives();
                        for (OnboardedInitiative i : onboardedInitiatives) {
                            if (checkDate(trxDateTime, i.getActiveTimeIntervals())) {
                                initiatives.add(i.getInitiativeId());
                            }
                        }
                    }
                    return Flux.fromIterable(initiatives);
                });
    }

    //scroll the ActiveTimeInterval list from the end to facilitate the exit of the for-cycle
    boolean checkDate(LocalDateTime trxDate, List<ActiveTimeInterval> timeIntervals){
        for (int i = timeIntervals.size()-1; i>=0;i--) {
            LocalDateTime start = timeIntervals.get(i).getStartInterval();
            LocalDateTime end = timeIntervals.get(i).getEndInterval();
            if (!trxDate.isBefore(start) && (end == null || !trxDate.isAfter(end))) {
                return true;
            }
        }
        return false;
    }
}