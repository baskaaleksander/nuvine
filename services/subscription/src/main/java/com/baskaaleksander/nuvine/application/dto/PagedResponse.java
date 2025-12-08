package com.baskaaleksander.nuvine.application.dto;

import java.util.Collection;

public record PagedResponse<T>(
        Collection<T> content,
        Integer totalPages,
        long totalElements,
        Integer size,
        Integer page,
        boolean last,
        boolean next
) {
}
