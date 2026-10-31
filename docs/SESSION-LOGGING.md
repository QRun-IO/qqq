# Authentication session logging

The OAuth2 module omits session UUIDs and callback state values from its log
events, including refresh and logout. Token rejection logs retain
only a known OAuth error category (or `unknown`) and HTTP status, with existing
public endpoint/client metadata. Provider `error_description` is no longer logged
or returned as the authentication exception message; rejected exchanges now use
the fixed message `OAuth2 token request failed`. Authentication failure semantics
are unchanged. Real DEBUG appender regressions cover expiry, unmatched state,
logout and a provider that echoes credential canaries in both error fields.

The shared logger's automatic `session.id` field now contains `sha256:` followed
by the SHA-256 digest of UTF-8 `qqq.log.session.v1:` plus the session UUID. It keeps
stable correlation across events without emitting the replayable UUID; missing
sessions/UUIDs produce `unknown`. Consumers that previously matched this field
directly against stored session UUIDs must migrate to the documented correlation.
This unkeyed digest is suitable for high-entropy random session UUIDs, not for
anonymizing low-entropy identifiers. The existing user and backend variant fields
retain their semantics. TABLE_BASED and Auth0 revalidation events also stop
concatenating their raw credential-bearing `idReference` values.

The tests cover these explicit authentication messages and automatic session context;
they do not claim to audit every exception or downstream logging sink in the framework.
