# Cross-cutting contracts

This is the index of the shared hooks that core offers to qbits, modules and clients. For each
contract it gives where it lives, who owns it, and which document defines it. Add a row whenever a
PR introduces a hook that more than one consumer uses. Rename or move a contract only before GA, or
with a deprecation period (see #1156 for the planned package moves).

## UI API and clients
| Contract | Where | Owner | Doc |
|---|---|---|---|
| UI API (`/qqq/v1`), versioning and contract test | `qqq-middleware-javalin` `specs.v1`, `tools.UiApiContract`, `OpenApiCompatibilityChecker` | UI API | `UI-API-CHANGELOG.md` |
| UI API extensions (module and qbit operations) | `specs.extensions.MiddlewareSpecExtensionsMetaData`, `AbstractJsonEndpointSpec` | UI API | `UI-API-EXTENSIONS.md` §1 |
| In-process invocation (`UiApiInvoker`, `UiApiCredentials`, `UiApiAiDataGateway`) | `specs.invoke` | UI API | `UI-API-EXTENSIONS.md` §5 |
| Client rendering of widgets, components, adornments and hints | OpenAPI schemas | UI API | `UI-API-CLIENTS.md` |
| Client channels and deny walls (assistant, sftp, token) | core `model.metadata.permissions.ClientChannelsMetaData`, `QPermissionRules.deniedClients` | UI API | `UI-API-CLIENT-CHANNELS.md` |
| Scoped credentials (`QCredentialScope` on `QSession`) | core `model.session` | UI API, with qbit-authentik | `UI-API-CLIENT-CHANNELS.md` (scoped credentials) |
| Raw-route audit and strict mode | `specs.UiApiRouteAuditor`, `QApplicationJavalinServer.withStrictUiApiRoutes` | UI API | `UI-API-EXTENSIONS.md` §2 |
| UI API / model API parity ("API = admin parity": `UiApiModelApiParityChecker`, `ParityExclusions`) | `qqq-middleware-api` `actions` | UI API | `UI-API-MODEL-API-PARITY.md` |

## Records and features
| Contract | Where | Owner | Doc |
|---|---|---|---|
| Record features metadata and feature permissions (`TableFeaturePermissionsMetaData`, `RecordFeaturePermissions`) | core `model.metadata.permissions`, `actions.collaboration` | collaboration | `COLLABORATION.md` §2 |
| Record references and security inheritance (`RecordAccessCheckerInterface`, `RecordReferenceSecurityHelper`, `RecordReferenceCleanupInterface`) | core `actions.collaboration` (moving to `actions.records.references`, #1156) | collaboration | `COLLABORATION.md` §3 |
| Runtime metadata overlay (`RuntimeMetaDataProviderInterface`) | core `actions.collaboration` | collaboration | `COLLABORATION.md` §5 |
| Followers and user sessions (`RecordFollowersProviderInterface`, `UserSessionProviderInterface`) | core `actions.collaboration` | collaboration | `COLLABORATION.md` §7 |
| Related-record picker operations | `specs.v1` | picker | `RELATED-RECORD-PICKER.md` |
| Data analysis (aggregates, pivots) | core aggregate actions, `specs.v1` | data-analysis | `DATA-ANALYSIS.md` |

## Data protection
| Contract | Where | Owner | Doc |
|---|---|---|---|
| Field access tiers (`FieldAccessHelper`, `accessTier`, masking) | core | secured-data | `SECURED-DATA.md` §2–3 |
| Data classification (`DataClassification`) | core `actions.ai` (moving to `model.metadata.security`, #1156) | secured-data | `SECURED-DATA.md` §5 |
| Audit value protection hook and token helper | core | secured-data, with qbit-worm-audit | `SECURED-DATA.md` §14 |
| AI exposure (`AiExposure`, `AiExposurePolicy`), folding into field access | core `actions.ai` | assistant, with secured-data | `SECURED-DATA.md` §3 |
| AI tool kernel data gateway (`AiDataGateway`) | core `actions.ai` | UI API | `UI-API-EXTENSIONS.md` §5 |

## Platform services
| Contract | Where | Owner | Doc |
|---|---|---|---|
| Alerts and notifications (`QAlerts`, `AlertSinkInterface`, `NotificationSignal`) | core `alerts` | qbit-alerts | QRun-IO/qqq#1090; qbit-alerts `docs/` |
| Webhook signing and verification (`WebhookSigner`, `WebhookVerifier`) | core `utils.webhooks` | qbit-webhooks | qbit-webhooks `docs/` |
| User preferences sections and actions | core (#1162) | preferences | `#1162` |
| In-app help (help content, search, coverage) | core, `specs.v1` | help content | `HELP-CONTENT.md` |
| Durable jobs | core (#1171) | durable jobs | `#1171` spec |
