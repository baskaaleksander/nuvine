package com.baskaaleksander.nuvine.domain.model;

import lombok.Getter;

public enum EmailTemplates {

    EMAIL_VERIFICATION("email-verification.html", "Email Verification - nuvine"),
    PASSWORD_RESET("password-reset.html", "Password Reset - nuvine"),
    USER_REGISTERED("user-registered.html", "Welcome to nuvine!"),
    WORKSPACE_MEMBER_ADDED("workspace-member-added.html", "You have been added to workspace! - nuvine"),
    WORKSPACE_MEMBER_INVITED("workspace-member-invited.html", "You've been invited to join a workspace! - nuvine"),
    PAYMENT_ACTION_REQUIRED("payment-action-required.html", "Payment Action Required - nuvine");

    @Getter
    private final String templateName;
    @Getter
    private final String subject;

    EmailTemplates(String templateName, String subject) {
        this.templateName = templateName;
        this.subject = subject;
    }

}
