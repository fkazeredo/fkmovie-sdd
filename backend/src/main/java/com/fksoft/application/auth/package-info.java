/**
 * Authentication module (SPEC-0003, ADR 0005): users, credentials, JWT issuance, refresh
 * token rotation and login rate limiting. Public API of the module: the domain events
 * ({@code UserLoggedIn}, {@code UserLoggedOut}, {@code PasswordChanged}) — no facade until
 * specs 0004/0005 introduce consumers.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Authentication")
package com.fksoft.application.auth;
