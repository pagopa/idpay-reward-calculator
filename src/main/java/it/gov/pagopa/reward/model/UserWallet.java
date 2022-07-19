package it.gov.pagopa.reward.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserWallet {

    private String id;
    private String userId;
    private String initiativeId;
    private LocalDate acceptanceDate;
    private Long budget;
    private List<OnboardedInitiative> accrued;
    private Long trxCount;
    private String status;
    private List<String> hpanActive;

    public UserWallet(String userId, String initiativeId, LocalDate acceptanceDate, Long budget, List<OnboardedInitiative> accrued, Long trxCount, String status, List<String> hpanActive) {
        this.id=userId.concat(initiativeId);
        this.userId = userId;
        this.initiativeId = initiativeId;
        this.acceptanceDate = acceptanceDate;
        this.budget = budget;
        this.accrued = accrued;
        this.trxCount = trxCount;
        this.status = status;
        this.hpanActive = hpanActive;
    }
}
