package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.PaymentStatus;
import lombok.*;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PaymentFilterRequest extends PaginationRequest {

    private LocalDate startDate = LocalDate.now().minusYears(1);

    private LocalDate endDate = LocalDate.now().plusDays(1);

    private PaymentStatus status;

    @Builder
    public PaymentFilterRequest(
            Integer page,
            Integer size,
            String sortField,
            Sort.Direction direction,
            LocalDate startDate,
            LocalDate endDate,
            PaymentStatus status
    ) {
        super();
        super.setPage(page != null ? page : 1);
        super.setSize(size != null ? size : 10);
        super.setSortField(sortField != null ? sortField : "createdAt");
        super.setDirection(direction != null ? direction : Sort.Direction.DESC);
        this.startDate = startDate != null ? startDate : LocalDate.now().minusYears(1);
        this.endDate = endDate != null ? endDate : LocalDate.now().plusDays(1);
        this.status = status;
    }
}
