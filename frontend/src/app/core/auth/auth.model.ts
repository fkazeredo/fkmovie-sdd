/** Authentication domain types mirroring the backend SPEC-0003/0004 contracts. */

/** Single active role per user (backend `Role`); roles do not stack in v1. */
export type Role = 'CUSTOMER' | 'OPERATOR' | 'ADMIN';

/** The authenticated principal, as returned in every auth response (`UserSummaryResponse`). */
export interface AuthUser {
  id: string;
  email: string;
  name: string;
  role: Role;
  emailVerified: boolean;
}

/** Body of POST /api/auth/login, /refresh and /api/users/register (`AuthTokensResponse`). */
export interface AuthTokens {
  accessToken: string;
  accessTokenExpiresAt: string;
  user: AuthUser;
}

/** Body of POST /api/users/verify-email (`VerifyEmailResponse`). */
export interface VerifyEmailResult {
  email: string;
  emailVerified: boolean;
}
