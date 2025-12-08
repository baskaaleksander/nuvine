package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.AggregationGranularity;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UsageAggregationRequest {

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Builder.Default
    private AggregationGranularity granularity = AggregationGranularity.DAILY;
}
