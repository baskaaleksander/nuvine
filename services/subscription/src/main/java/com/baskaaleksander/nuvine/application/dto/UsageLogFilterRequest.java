package com.baskaaleksander.nuvine.application.dto;

import lombok.*;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UsageLogFilterRequest extends PaginationRequest {

    private LocalDate startDate;

    private LocalDate endDate;

    private String provider;

    private String model;

    @Builder
    public UsageLogFilterRequest(
            Integer page,
            Integer size,
            String sortField,
            Sort.Direction direction,
            LocalDate startDate,
            LocalDate endDate,
            String provider,
            String model
    ) {
        super();
        super.setPage(page != null ? page : 1);
        super.setSize(size != null ? size : 10);
        super.setSortField(sortField != null ? sortField : "occurredAt");
        super.setDirection(direction != null ? direction : Sort.Direction.DESC);
        this.startDate = startDate;
        this.endDate = endDate;
        this.provider = provider;
        this.model = model;
    }
}
