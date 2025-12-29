package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.domain.service.ChatService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    @PreAuthorize("@chatAccess.canCreateMessage(#request.conversationId, #jwt.getSubject(), #request.projectId, #request.workspaceId)")
    @PostMapping("/completions")
    @RateLimiting(
            name = "completion_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<ConversationMessageResponse> completions(
            @RequestBody @Valid CompletionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(chatService.completion(request, jwt.getSubject()));
    }

    @PreAuthorize("@chatAccess.canCreateMessage(#request.conversationId, #jwt.getSubject(), #request.projectId, #request.workspaceId)")
    @PostMapping(
            value = "/completions/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    @RateLimiting(
            name = "completion_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public SseEmitter completionStream(
            @RequestBody CompletionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        return chatService.completionStream(request, userId);
    }

    @GetMapping
    public ResponseEntity<List<UserConversationResponse>> getUserConversations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID projectId
    ) {
        return ResponseEntity.ok(chatService.getUserConversations(jwt.getSubject(), projectId));
    }

    @PreAuthorize("@chatAccess.canAccessChat(#conversationId, #jwt.getSubject())")
    @GetMapping("/{conversationId}")
    public ResponseEntity<PagedResponse<ConversationMessageResponse>> getMessages(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction
    ) {
        PaginationRequest request = new PaginationRequest(page, size, sortField, direction);
        return ResponseEntity.ok(chatService.getMessages(conversationId, jwt.getSubject(), request));
    }
}
