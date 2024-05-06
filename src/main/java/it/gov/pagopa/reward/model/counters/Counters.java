package it.gov.pagopa.reward.model.counters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldNameConstants
public class Counters {
    @Builder.Default
    private Long trxNumber = 0L;
    @Builder.Default
    private Long totalRewardCents = 0L;
    @Builder.Default
    private Long totalAmountCents = 0L;

    public abstract static class CountersBuilder<C extends Counters, B extends CountersBuilder<C, B>> {
        protected C hiddenBuild(){
            return this.build();
        }
    }
}
