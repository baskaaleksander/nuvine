package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.AggregationGranularity;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Getter
@Setter
public class UsageAggregationRequest {

    private LocalDate startDate = LocalDate.now().minusDays(30);

    private LocalDate endDate = LocalDate.now();

    private AggregationGranularity granularity = AggregationGranularity.DAILY;

    @Builder
    public UsageAggregationRequest(
            LocalDate startDate,
            LocalDate endDate,
            AggregationGranularity granularity
    ) {
        this.startDate = startDate != null ? startDate : LocalDate.now().minusDays(30);
        this.endDate = endDate != null ? endDate : LocalDate.now();
        this.granularity = granularity != null ? granularity : AggregationGranularity.DAILY;
    }
}
