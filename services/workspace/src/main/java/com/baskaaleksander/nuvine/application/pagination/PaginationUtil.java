package com.baskaaleksander.nuvine.application.pagination;

import com.baskaaleksander.nuvine.application.dto.PaginationRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaginationUtil {

    public static Pageable getPageable(PaginationRequest request) {
        return PageRequest.of(request.getPage(), request.getSize(), request.getDirection(), request.getSortField());
    }
}
