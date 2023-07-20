package it.gov.pagopa.reward.model.counters;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
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
    private String userId;
    @NonNull
    private String initiativeId;

    /** Used in Sync trx flow in order to handle throttling on user's initiative counter.<br/>
     * See <a href="https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/674169248/Transazioni+sincrone">Confluence page</a>
     * */
    private LocalDateTime updateDate;
    /** Used in Sync trx flow in order to handle transactional updates of {@link it.gov.pagopa.reward.model.TransactionProcessed} and this entity.<br/>
     * See <a href="https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/727778640/Gestione+transazionalit+salvataggio+transazione+e+contatori+aggiornati">Confluence page</a>.
     * */
    private List<String> updatingTrxId;

    private boolean exhaustedBudget;
    @Builder.Default
    private Map<String, Counters> dailyCounters = new HashMap<>();
    @Builder.Default
    private Map<String, Counters> weeklyCounters = new HashMap<>();
    @Builder.Default
    private Map<String, Counters> monthlyCounters = new HashMap<>();
    @Builder.Default
    private Map<String, Counters> yearlyCounters = new HashMap<>();

    public UserInitiativeCounters(@NonNull String userId, @NonNull String initiativeId){
        this.id=buildId(userId, initiativeId);
        this.userId=userId;
        this.initiativeId=initiativeId;
        this.updateDate = LocalDateTime.now();

        // for some reason, lombok is changing the code letting null these fields when using this constructor
        this.dailyCounters = new HashMap<>();
        this.weeklyCounters = new HashMap<>();
        this.monthlyCounters = new HashMap<>();
        this.yearlyCounters = new HashMap<>();
    }

    public static String buildId(String userId, String initiativeId) {
        return "%s_%s".formatted(userId, initiativeId);
    }

    @SuppressWarnings("squid:S1452")
    public static UserInitiativeCountersBuilder<?,?> builder(String userId, String initiativeId){
        return UserInitiativeCounters.hiddenBuilder()
                .id(buildId(userId, initiativeId))
                .userId(userId)
                .initiativeId(initiativeId)
                .updateDate(LocalDateTime.now());
    }

    public abstract static class UserInitiativeCountersBuilder<C extends UserInitiativeCounters, B extends UserInitiativeCountersBuilder<C, B>> extends CountersBuilder<C,B> {

        public B userId(String userId){
            this.userId=userId;
            this.id=buildId(this.userId, this.initiativeId);
            return self();
        }

        public B initiativeId(String initiativeId){
            this.initiativeId=initiativeId;
            this.id=buildId(this.userId, this.initiativeId);
            return self();
        }

        @Override
        public C build() {
            C out = this.hiddenBuild();
            out.setId(buildId(out.getUserId(), out.getInitiativeId()));
            return out;
        }
    }
}
