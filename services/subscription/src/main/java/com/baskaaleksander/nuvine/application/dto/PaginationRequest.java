package com.baskaaleksander.nuvine.application.dto;

import lombok.*;
import org.springframework.data.domain.Sort;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaginationRequest {

    private Integer page = 1;

    private Integer size = 10;

    private String sortField = "id";

    private Sort.Direction direction = Sort.Direction.DESC;

}