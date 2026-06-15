package com.fksoft.application.notification;

/**
 * Catalog of email templates (SPEC-0006). Each entry maps to a Thymeleaf template under
 * {@code templates/email/} and a subject {@code MessageSource} key. Only the templates an
 * implemented flow needs exist; others arrive with their producing specs (Rule Zero).
 */
public enum EmailTemplate {
    VERIFICATION("verification", "email.verification.subject"),
    PASSWORD_RESET("password-reset", "email.password-reset.subject"),
    INVITATION("invitation", "email.invitation.subject"),
    TICKETS("confirmation", "email.confirmation.subject"),
    CANCELLATION("cancellation", "email.cancellation.subject");

    private final String templateName;
    private final String subjectKey;

    EmailTemplate(String templateName, String subjectKey) {
        this.templateName = templateName;
        this.subjectKey = subjectKey;
    }

    public String templateName() {
        return templateName;
    }

    public String subjectKey() {
        return subjectKey;
    }
}
