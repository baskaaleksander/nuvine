package com.baskaaleksander.nuvine.domain.model;

import lombok.Getter;

public enum EmailTemplates {

    EMAIL_VERIFICATION("email-verification.html", "Email Verification - nuvine"),
    PASSWORD_RESET("password-reset.html", "Password Reset - nuvine"),
    USER_REGISTERED("user-registered.html", "Welcome to nuvine!"),
    WORKSPACE_MEMBER_ADDED("workspace-member-added.html", "You have been added to workspace! - nuvine");

    @Getter
    private final String templateName;
    @Getter
    private final String subject;

    EmailTemplates(String templateName, String subject) {
        this.templateName = templateName;
        this.subject = subject;
    }

}
