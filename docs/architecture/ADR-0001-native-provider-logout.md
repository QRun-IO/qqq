# ADR-0001: Optional native provider logout

**Status:** Accepted capability; rollout policy awaiting user choice
**Date:** 2026-11-01
**Deciders:** Implementation team; user decides when company sign-out runs

## Context

Real Authentik acceptance verifies PKCE login, the QQQ principal, local logout
and rejection of the old QQQ cookie. With provider SSO retained, the tested
Authentik version can reuse the previous identity despite `prompt=login`.
Its implementation also ignores zero when evaluating `max_age`. Parameters
alone therefore cannot establish reliable operator switching in this setup.

The client must use a declared, versioned API capability. Local QQQ logout must
remain available when the provider or browser fails. Existing local-only logout
and API 1.9 clients retain their existing contract.

## Decision

Add optional native RP-initiated logout in API 1.10, using typed opt-in input and
optional output on the existing logout operation. Advertise it only for an
explicitly configured eligible OAuth2 provider. Retain the original ID token
encrypted in system-only session storage with a distinct purpose and binding to
the native session, user and provider. Release a hint only to the authenticated
matching cookie session and an explicitly permitted post-logout callback.

The Android client validates the returned provider/discovery/callback binding,
uses AppAuth end-session handling and an independent single-use state, and
never treats a logout callback as login. It clears local credentials and private
UI state independently of provider completion. Sensitive hints are not logged or
deliberately persisted by the application; SDK Intent and browser-query handling
are documented boundaries.

Company sign-out on every Sign out versus only on explicit Switch account is
still a user policy decision. This capability does not select that default or
authorize terminating a real company SSO session during development.

## Options considered

- Rely on `prompt` or add `max_age=0`: rejected as an interoperability guarantee
  for the observed provider behavior.
- Patch Authentik: rejected as the client integration's default requirement.
- Standard RP logout: accepted as an explicit provider-session operation, with
  local logout remaining independent.
- Keep SSO and require account confirmation: remains a rollout-policy option;
  explicit account switching can use the same provider logout capability.

## Consequences and verification

- Provider post-logout callback registration and encryption-key configuration
  are required; login callback registration alone is insufficient.
- Provider discovery or revocation errors must not skip local invalidation.
- Tests must cover hint binding and confidentiality, migration, mixed identity
  sources, cleanup failures, stale callbacks and browser cancellation.
- Real provider acceptance must prove the chosen A-to-B operator handoff. The
  original retained-SSO failure remains evidence and is not relabeled as a pass.
- A new scoped security review is required for the resulting immutable patch;
  earlier sealed scans do not cover this capability.
