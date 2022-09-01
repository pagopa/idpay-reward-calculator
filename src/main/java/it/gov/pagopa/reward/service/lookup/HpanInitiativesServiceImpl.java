package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.dto.mapper.HpanInitiativeDTO2InitialEntityMapper;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class HpanInitiativesServiceImpl implements HpanInitiativesService{

    private final HpanInitiativeDTO2InitialEntityMapper hpanInitiativeDTO2InitialEntityMapper;

    public HpanInitiativesServiceImpl(HpanInitiativeDTO2InitialEntityMapper hpanInitiativeDTO2InitialEntityMapper) {
        this.hpanInitiativeDTO2InitialEntityMapper = hpanInitiativeDTO2InitialEntityMapper;
    }

    @Override
    public Mono<HpanInitiatives> hpanInitiativeUpdateInformation(Pair<HpanInitiativeDTO, Mono<HpanInitiatives>> hpanInitiativePair) {
        HpanInitiativeDTO hpanInitiativeDTO = hpanInitiativePair.getFirst();
        Mono<HpanInitiatives> hpanRetrieved = hpanInitiativePair.getSecond();

        return hpanRetrieved
                .defaultIfEmpty(hpanInitiativeDTO2InitialEntityMapper.apply(hpanInitiativeDTO))
                .mapNotNull(h-> evaluate(h, hpanInitiativeDTO));
    }

    private HpanInitiatives evaluate(HpanInitiatives hpanInitiatives, HpanInitiativeDTO dto){
        return switch (dto.getOperationType()) {
            case HpanInitiativeConstants.ADD_INSTRUMENT -> addedHpanInitiative(hpanInitiatives, dto);
            case HpanInitiativeConstants.DELETE_INSTRUMENT -> deletedHpanInitiative(hpanInitiatives, dto);
            default -> null;
        };
    }
    private HpanInitiatives addedHpanInitiative(HpanInitiatives hpanInitiatives, HpanInitiativeDTO dto){
        List<OnboardedInitiative> onboardedInitiatives = hpanInitiatives.getOnboardedInitiatives();
        log.trace("[ADD_HPAN] Added evaluation for hpan: {}",dto.getHpan());
        if (onboardedInitiatives != null
                && onboardedInitiatives.stream().map(OnboardedInitiative::getInitiativeId).filter(id -> id.equals(dto.getInitiativeId())).count() == 1L) {
            List<ActiveTimeInterval> intervalsList = hpanInitiatives.getOnboardedInitiatives().stream()
                    .filter(h -> h.getInitiativeId().equals(dto.getInitiativeId()))
                    .map(OnboardedInitiative::getActiveTimeIntervals)
                    .toList().get(0);
            if (intervalsList.stream().noneMatch(activeTimeInterval -> dto.getOperationDate().isBefore(activeTimeInterval.getStartInterval()))) {
                log.trace("[ADD_HPAN] [HPAN_WITH_INITIATIVE] [INITIATIVE_ALREADY_PRESENT] Added evaluation for hpan: %s and add initiative: %s".formatted(dto.getHpan(),dto.getInitiativeId()));
                OnboardedInitiative onboardedInitiative = hpanInitiatives.getOnboardedInitiatives().stream()
                        .filter(h -> h.getInitiativeId().equals(dto.getInitiativeId())).toList().get(0);

                List<ActiveTimeInterval> activeTimeIntervalsList = onboardedInitiative.getActiveTimeIntervals();

                LocalDateTime lastActivate = Collections.max(activeTimeIntervalsList.stream().map(ActiveTimeInterval::getStartInterval).toList());
                ActiveTimeInterval lastActiveInterval = activeTimeIntervalsList.stream().filter(activeTimeInterval -> activeTimeInterval.getStartInterval().equals(lastActivate)).toList().get(0);

                if (lastActiveInterval.getEndInterval() != null && lastActiveInterval.getEndInterval().isBefore(dto.getOperationDate())) {
                    onboardedInitiative.getActiveTimeIntervals()
                            .add(ActiveTimeInterval.builder()
                                    .startInterval(dto.getOperationDate().with(LocalTime.MIN).plusDays(1L))
                                    .build());
                    return hpanInitiatives;
                }
            }
            return null;
        } else {
            log.trace("[ADD_HPAN] [HPAN_WITHOUT_INITIATIVE] Added evaluation for hpan: %s and add initiative: %s".formatted(dto.getHpan(),dto.getInitiativeId()));
            OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                    .initiativeId(dto.getInitiativeId())
//                    .acceptanceDate(dto.getOperationDate().toLocalDate())
                    .status("ACCEPTED")
                    .activeTimeIntervals(List.of(ActiveTimeInterval.builder().startInterval(dto.getOperationDate().with(LocalTime.MIN).plusDays(1L)).build()))
                    .build();

            if(hpanInitiatives.getOnboardedInitiatives() == null) {
                hpanInitiatives.setOnboardedInitiatives(new ArrayList<>());
            }

            hpanInitiatives.getOnboardedInitiatives().add(onboardedInitiative);
            return hpanInitiatives;
        }
    }

    private HpanInitiatives deletedHpanInitiative(HpanInitiatives hpanInitiatives, HpanInitiativeDTO dto) {
        if (hpanInitiatives.getHpan() != null) {
            List<OnboardedInitiative> onboardedInitiatives = hpanInitiatives.getOnboardedInitiatives();
            log.trace("[DELETED_HPAN] Is presente in db the follow hpan: {}",dto.getHpan());
            if (onboardedInitiatives != null
                    && onboardedInitiatives.stream().map(OnboardedInitiative::getInitiativeId).filter(id -> id.equals(dto.getInitiativeId())).count() == 1L) {
                OnboardedInitiative onboardedInitiative = onboardedInitiatives.stream()
                        .filter(h -> h.getInitiativeId().equals(dto.getInitiativeId())).toList().get(0);
                log.trace("[DELETED_HPAN] [HPAN_WITH_INITIATIVE] [INITIATIVE_IS_PRESENT] The hpan: %s, contain the initiative: %s".formatted(dto.getHpan(),dto.getInitiativeId()));
                List<ActiveTimeInterval> activeTimeIntervalsList = onboardedInitiative.getActiveTimeIntervals();

                LocalDateTime lastActivate = Collections.max(activeTimeIntervalsList.stream().map(ActiveTimeInterval::getStartInterval).toList());
                List<ActiveTimeInterval> lastActiveInterval = activeTimeIntervalsList.stream().filter(activeTimeInterval -> activeTimeInterval.getStartInterval().equals(lastActivate)).toList();

                if (dto.getOperationDate().isAfter(lastActivate)
                        && lastActiveInterval.get(0).getEndInterval() == null) {

                    onboardedInitiative.getActiveTimeIntervals().remove(lastActiveInterval.get(0));

                    onboardedInitiative.getActiveTimeIntervals().add(ActiveTimeInterval.builder()
                            .startInterval(lastActivate)
                            .endInterval(dto.getOperationDate().with(LocalTime.MAX))
                            .build());

                    return hpanInitiatives;
                }
            }
        }
        return null;
    }
}
