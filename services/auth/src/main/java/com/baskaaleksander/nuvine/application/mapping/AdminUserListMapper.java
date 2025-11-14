package com.baskaaleksander.nuvine.application.mapping;

import com.baskaaleksander.nuvine.application.dto.AdminUserListResponse;
import com.baskaaleksander.nuvine.domain.model.User;
import org.springframework.stereotype.Component;

@Component
public class AdminUserListMapper {

    public AdminUserListResponse toAdminUserListResponse(User user) {
        return new AdminUserListResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.isEmailVerified(),
                user.isOnboardingCompleted()
        );
    }
}
