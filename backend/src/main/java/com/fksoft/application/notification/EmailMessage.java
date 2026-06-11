package com.fksoft.application.notification;

/** A rendered email ready to send: recipient, subject, and HTML + plain-text bodies. */
public record EmailMessage(String to, String subject, String htmlBody, String textBody) {}
