package com.baskaaleksander.nuvine.application.dto;

import java.util.List;

public record WorkspaceMembersResponse(
        List<WorkspaceMemberResponse> members,
        Long count
) {
}
