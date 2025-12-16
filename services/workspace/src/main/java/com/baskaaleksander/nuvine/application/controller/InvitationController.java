package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.InvitationResponseRequest;
import com.baskaaleksander.nuvine.domain.service.WorkspaceMemberInviteTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final WorkspaceMemberInviteTokenService workspaceMemberInviteTokenService;

    @PostMapping("/{token}")
    public ResponseEntity<Void> respondToInvitation(
            @PathVariable String token,
            @RequestBody @Valid InvitationResponseRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String email = jwt.getClaimAsString("email");
        workspaceMemberInviteTokenService.respondToInvitation(token, request.action(), email);
        return ResponseEntity.ok().build();
    }
}
