package com.fksoft.domain.auth;

/** Single active role per user (SPEC-0003: roles do not stack in v1). */
public enum Role {
    CUSTOMER,
    OPERATOR,
    ADMIN
}
