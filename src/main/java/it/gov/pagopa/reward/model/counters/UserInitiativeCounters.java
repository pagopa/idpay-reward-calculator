package it.gov.pagopa.reward.model.counters;

import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.trx.LastTrxInfoDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true, builderMethodName = "hiddenBuilder", buildMethodName = "hiddenBuild")
@ToString(callSuper = true)
@FieldNameConstants
@Document(collection = "user_initiative_counters")
public class UserInitiativeCounters extends Counters {

    @Id
    private String id;

    /** Used in Async trx flow in order to handle transactional updates of {@link it.gov.pagopa.reward.model.TransactionProcessed} and this entity.<br/>
     * See <a href="https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/708576249/Gestione+transazionalit+elaborazione+transazione+e+aggiornamento+contatori+in+caso+di+pi+iniziative">Confluence page</a>.
     * */
    private long version;

    @NonNull
    private String entityId;
    @NonNull
    private InitiativeGeneralDTO.BeneficiaryTypeEnum entityType;
    @NonNull
    private String initiativeId;

    private LocalDateTime updateDate;
    private TransactionDTO pendingTrx;
    @Builder.Default
    private List<LastTrxInfoDTO> lastTrx = new ArrayList<>();

    private boolean exhaustedBudget;

    @Builder.Default
    private Map<String, Counters> dailyCounters = new HashMap<>();
    @Builder.Default
    private Map<String, Counters> weeklyCounters = new HashMap<>();
    @Builder.Default
    private Map<String, Counters> monthlyCounters = new HashMap<>();
    @Builder.Default
    private Map<String, Counters> yearlyCounters = new HashMap<>();

    public UserInitiativeCounters(@NonNull String entityId, @NonNull InitiativeGeneralDTO.BeneficiaryTypeEnum entityType, @NonNull String initiativeId){
        this.id=buildId(entityId, initiativeId);
        this.entityId = entityId;
        this.entityType = entityType;
        this.initiativeId=initiativeId;
        this.updateDate = LocalDateTime.now();

        // for some reason, lombok is changing the code letting null these fields when using this constructor
        this.dailyCounters = new HashMap<>();
        this.weeklyCounters = new HashMap<>();
        this.monthlyCounters = new HashMap<>();
        this.yearlyCounters = new HashMap<>();
        this.lastTrx = new ArrayList<>();
    }
    public UserInitiativeCounters(@NonNull String entityId, @NonNull String initiativeId){
        this(entityId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF, initiativeId);
    }

    public static String buildId(String entityId, String initiativeId) {
        return "%s_%s".formatted(entityId, initiativeId);
    }

    @SuppressWarnings("squid:S1452")
    public static UserInitiativeCountersBuilder<?,?> builder(String entityId, InitiativeGeneralDTO.BeneficiaryTypeEnum entityType,String initiativeId){
        return UserInitiativeCounters.hiddenBuilder()
                .id(buildId(entityId, initiativeId))
                .entityId(entityId)
                .entityType(entityType)
                .initiativeId(initiativeId)
                .updateDate(LocalDateTime.now());
    }

    public abstract static class UserInitiativeCountersBuilder<C extends UserInitiativeCounters, B extends UserInitiativeCountersBuilder<C, B>> extends CountersBuilder<C,B> {

        public B entityId(String entityId){
            this.entityId =entityId;
            this.id=buildId(this.entityId, this.initiativeId);
            return self();
        }

        public B initiativeId(String initiativeId){
            this.initiativeId=initiativeId;
            this.id=buildId(this.entityId, this.initiativeId);
            return self();
        }

        @Override
        public C build() {
            C out = this.hiddenBuild();
            out.setId(buildId(out.getEntityId(), out.getInitiativeId()));
            return out;
        }
    }
}
