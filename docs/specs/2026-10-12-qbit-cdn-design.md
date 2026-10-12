# QQQ CDN qbit: Design Spec

Status: **Owner decisions recorded 2026-10-12. Ready for review.** · drafted 2026-10-12 · Author: James Maes (drafted with Claude)
Targets: QQQ 4.1.x (one small response-header hook in `qqq-middleware-javalin`) and a new repo, `QRun-IO/qbit-cdn`. The first consumer is kof22:
- `Website-Backend` and `Website-Frontend`
- `cluster-gitops` tenants/kof22
- Praetor `dns/route53`, `unifi/`, and a new `terraform/cdn-cloudfront/`

> This spec is a design. Phase P0 (origin cleanup and the `upload.kof22.com` split) is being implemented. No AWS resources have been created; that happens in P2. The open items O1 and O3–O8 were answered on 2026-10-12. O2, the plan subscription, is the owner's own action when the distribution exists (§12.2).

## Owner decisions (2026-10-12)

These decisions override anything below that contradicts them. The sections they affect have been updated.

| # | Decision | Effect on the design |
|---|---|---|
| D1 | **AWS CloudFront first**, on the **flat-rate Pro plan ($15/mo)**. Cloudflare is not used. | CloudFront adapter first (§4.3, §8). Fastly, Bunny, Cloudflare and Souin stay in the SPI. Souin/Varnish is the e2e test target. |
| D2 | **DNS stays in Route53, managed by octoDNS in Praetor.** | The Cloudflare zone-move question is moot. Route53 ALIAS records point to CloudFront, including the apex (§8.1). |
| D3 | **Origin protection, CloudFront version.** A dedicated `cdn` Traefik entry point on its own WAN port, reachable only from CloudFront's origin-facing ranges (enforced at UniFi as IaC), **plus** a secret origin header checked at Traefik. VPC origins do not apply. | §7.1, §7.2. The trade-offs against a tunnel are in §7.1. |
| D4 | Hostnames, in order: `s3` → `www` assets and images → `api` → `www` HTML. **Never** `admin`, `auth`, `minio-console` or the DSM hosts. | §5.1, §8.4. |
| D5 | **The real client IP comes from `CloudFront-Viewer-Address`**, and is trusted **only** when the origin secret is valid. The paused Traefik DaemonSet / `externalTrafficPolicy: Local` change is **still wanted for hostnames that are not behind the CDN**. | §7.3. |
| D6 | Media **stays on MinIO**, with CloudFront in front and immutable cache headers. | §4.6, §5.1. |
| D7 | **No native image transforms.** Cache the existing renditions and Next's `/_next/image` output at the edge. CloudFront Functions or Lambda@Edge transforms can come later, optionally. | §6.2. A small CloudFront Function normalizes the `Accept` header for `/_next/image` (§5.2). |
| D8 | **Yes, cache HTML in Phase 4.** Use ISR, and revalidate every Next pod **before** the CloudFront invalidation. | §6.3. |
| D9 | **Edge configuration is OpenTofu in Praetor.** It uses the AWS provider with the existing `kingsrook_root_admin` profile and account-guard conventions. The qbit only emits headers, invalidates, and reads config for a drift view. | §8.2. |
| D10 | A new repo, **`QRun-IO/qbit-cdn`**, plus one small generic response-header hook in QQQ core. | §3.1. |
| D11 | Budget: **$0–25/mo**. | §10. Expected cost is about $15–20/mo. |
| D12 | **No signed CDN URLs in v1.** Private media stays on MinIO presigned links that skip the edge. CloudFront **key groups** make signed URLs easy to add later. | §4.10. |
| D13 | **Privacy.** No CDN-injected scripts and no CDN visitor analytics. Name AWS as a processor on the privacy page. Keep the first-party hashed analytics. | §7.4. |
| D14 | **When a purge fails:** 5 retries with backoff, then DEAD plus a Data Health issue and an alert. Content corrects itself when the edge TTL expires. | §4.5. |
| D15 | **kof22 only for now.** Decide on qrun.io and Benchfinity after Phase 4. | §8.5. |

## 0. Summary

`qbit-cdn` lets any QQQ application put a CDN in front of its public site, public API and media without being tied to one CDN vendor. It does four things.

1. **Emits portable cache headers.** It sends standard `Cache-Control` for browsers, with `s-maxage` for shared caches. It also sends `CDN-Cache-Control` (RFC 9213) and a cache-tag header, but only for providers that support them. The values come from **cache policies** stored as normal QQQ records.
2. **Invalidates on change.** CMS inserts, updates and deletes arrive as ESB events, and the qbit turns them into **canonical tags** such as `post:123`, `blog:7:list` and `media:456`. The provider adapter then turns those tags into what its CDN understands:
   - on CloudFront, invalidation **paths** and **wildcards**, planned against a monthly invalidation budget
   - on Fastly, Bunny and Souin, tag purges.

   Requests are merged, rate-limited, retried, and recorded in a visible `cdnPurgeRequest` table.
3. **Builds URLs.** Media URLs stay the same, because the CDN fronts `s3.kof22.com` itself, so no stored URL changes. It also builds image URLs for `FILE_PREVIEW` and the Next.js loader. Signed URLs are part of the SPI but are not used in v1.
4. **Reports real data.** It imports edge statistics from CloudWatch (requests, bytes, error rates and, when available, cache hit rate) into `cdnEdgeStatDaily`. These feed a CDN widget in Admin → Health, which shows only imported or recorded data.

**What kof22 does today (§1.2):**
- There is no CDN.
- The Next.js site renders every page dynamically.
- Media objects have no `Cache-Control`, even though their keys are immutable and content-addressed.
- Nothing consumes the CMS ESB topic.
- Traefik sees SNAT addresses instead of real client IPs.

Phase 0 is about 3–4 days of origin cleanup that helps even before any CDN is in place.

**The CloudFront setup (§7, §8):**
- **One distribution** serves `kof22.com`, `www`, `api` and `s3`, on the flat-rate Pro plan.
- Its origin is `origin.kof22.com:8443`, a DNS-only record pointing to the WAN IP. Port 8443 is a new UniFi port-forward that only accepts traffic from CloudFront's origin-facing ranges, and it lands on a dedicated Traefik `cdn` entry point.
- On that entry point, a tiny **`origin-guard`** forwardAuth service checks the secret `X-Origin-Verify` header that CloudFront adds. Only when the secret is valid does it set `X-Real-IP` from `CloudFront-Viewer-Address`.
- Rollout is one hostname at a time. Rollback is reverting one Route53 record through octoDNS.

**Cost** is about **$15–20/mo**: the Pro plan, plus paid invalidations only if the budget planner allows them beyond the free 1,000 paths, plus any optional metrics. **Effort** is about **5.5–7.5 weeks** for P0–P4.

---

## 1. Context

### 1.1 Goals

- Faster first byte and asset loads for visitors far from the homelab, and less load on the residential uplink.
- One qbit that works with CloudFront, Cloudflare, Fastly, Bunny, Akamai, Azure Front Door, Google Cloud CDN or a self-hosted cache. Switching providers needs no app code changes.
- Correct by default. Nothing personalized or authenticated is ever cached at the edge, and published changes appear within about a minute.
- Only the CDN can use the protected origin path, and the real client IP is trusted only on that path.
- Follow the owner's qbit rules:
  - qbit data lives in standard QQQ tables, secured by permissions and locks
  - processes are table or record actions, never nav items
  - only real data is shown
  - the UI is readable at every width.

### 1.2 Current state (kof22, researched 2026-10-12)

| Area | Today | CDN relevance |
|---|---|---|
| DNS | The Route53 zone `kof22.com` has DNSSEC on. Records are managed by octoDNS in Praetor `dns/route53/zones/kof22.com.yaml`.<br>The zone, DNSSEC and IAM live in tofu `terraform/dns-route53`: profile `kingsrook_root_admin`, `us-east-1`, `allowed_account_ids = ["744245396565"]`, and local git-crypt state. Each credential is a narrow IAM user: Key A octoDNS, Key B cert-manager, Key C registrar.<br>`kof22.com`, `www`, `admin`, `api` (TTL 60) and the DSM hosts are A records to `50.122.5.146`. `s3`, `auth` and `minio-console` are CNAMEs to `www`. CAA allows only `letsencrypt.org`. | ALIAS and CNAME records to CloudFront go through octoDNS. The ACM validation CNAME must be in the octoDNS zone file, or octoDNS will remove it. CAA needs an Amazon entry. |
| Edge path | UniFi port-forward 443 → Cilium LB `10.100.22.64` → Traefik (1 replica). `externalTrafficPolicy` is left at `Cluster`, so traffic is SNAT'd. `forwardedHeaders.trustedIPs` is set to the RFC1918 ranges. | **Traefik cannot see source IPs**, so a Traefik `ipAllowList` of CloudFront ranges would not work today. The IP filter has to sit at UniFi (§7.1). |
| TLS | cert-manager issues Let's Encrypt certs through Route53 DNS-01. The Traefik default cert is the wildcard `kof22-wildcard-tls` (`*.kof22.com` plus the apex). | The origin keeps the LE wildcard, which covers `origin.kof22.com` and every forwarded Host. The viewer cert is from ACM in `us-east-1`. |
| Media | MinIO public bucket `kof22-website-files`, served at `https://s3.kof22.com/kof22-website-files/...`. Keys look like `media/{siteId}/{slug}/{sha256}/{original.ext,thumbnail.jpg,gallery.jpg,slideshow.jpg}`.<br>Uploads set **no `Cache-Control`**.<br>iOS uploads use presigned PUTs into the private bucket `kof22-website-incoming`, **signed for `https://s3.kof22.com`** (`SITE_API_S3_PUBLIC_ENDPOINT`). | Media is immutable. Presigned PUTs must move to a hostname that is not behind the CDN before `s3` cuts over (§5.1). |
| Site API | `api.kof22.com`. `SiteApiRouteProvider.finish()` sets `public, max-age=60` (300 for docs, feeds and the sitemap) and `no-store` on `/v1/me*` and `/v1/contact`. It strips `Set-Cookie` and adds `Vary: Origin`. Reads have no ETag and never return 304. | Almost ready for a CDN (§5.4). |
| Frontend | Next.js `output: 'standalone'`, 2 replicas, `force-dynamic` in the root layout. There is no ISR, no tags, no `cacheHandler` and no revalidation route. The `/_next/image` optimizer caches per pod. | Stage A (assets and images) works now. Stage B (HTML) needs ISR (§6). |
| ESB | Artemis topics `kof22.cms.tables` (INSERT/UPDATE/DELETE events for 16 CMS tables) and `kof22.cms.processes`. **Nothing consumes `kof22.cms.tables`.** | The invalidation pipeline subscribes here (§4.5). |
| Analytics | Privacy-first. The visitor hash is SHA-256(daily salt, site, IP, UA), and the IP is never stored. Geo is city-level. A SNAT'd IP is recorded as `GeoStatus.PROXY_IP`. | The real IP comes through the guard (§7.3). |
| QQQ core | `QJavalinImplementation` and the API middleware have no cache or ETag handling. Only `NextDashboardRouteProvider` and `BrandingAssetsRouteProvider` set `Cache-Control`. | Needs one small core hook (§3.1). |

### 1.3 Non-goals (v1)

- Caching `admin.*`, `auth.*`, the QQQ Next dashboard, `/v1/me*`, or any authenticated response.
- Edge compute as part of the qbit. The single CloudFront Function in §5.2 is infrastructure owned by Praetor, not qbit code.
- The app pushing CDN configuration. OpenTofu owns that (D9).
- Video delivery and streaming.
- Signed CDN URLs (D12).

---

## 2. Research summary

Sources are in §14. Prices are as of 2026-10-12. *verify* marks a claim that was not confirmed against a primary source in this session.

### 2.1 Provider comparison

| | AWS CloudFront | Cloudflare | Fastly | Bunny.net | Google Cloud CDN | Azure Front Door | Akamai | Self-hosted (Souin / Varnish) |
|---|---|---|---|---|---|---|---|---|
| **Purge by URL** | Yes (invalidation paths) | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| **Prefix / wildcard** | Yes. `*` only at the end, and a wildcard path counts as **1 path** for billing. | Yes (all plans) | via key | Yes | Yes (prefix, host) | Yes | via CP code | Varnish BAN |
| **Purge by tag** | **No** | Yes, all plans (`Cache-Tag`) | Yes (`Surrogate-Key`) | Yes (`CDN-Tag`) | Yes (`Cache-Tag`, 50 per object) | **No** | Yes (`Edge-Cache-Tag`) | Yes (Souin `Surrogate-Key`, Varnish `xkey`) |
| **Purge cost / limits** | **First 1,000 paths/month free**, then about $0.005 per path.<br>Quotas cap concurrent in-progress paths (wildcards are capped low, about 15 in progress, *verify*).<br>Invalidation is not instant (typically up to a few minutes, *verify*).<br>Whether flat-rate plans include invalidations: *verify*. | Free: 5 tag/prefix requests per minute, 100 items each | ~100k/h | not published | 500/min | ~100 concurrent | plan | none |
| **Signed URLs** | Yes. Key groups, canned or custom policy, URLs or cookies. | WAF HMAC or a Worker | VCL or Compute | Token auth (HMAC-SHA256) | Yes | Premium | EdgeAuth | imgproxy only |
| **Image transforms** | None native. Use CloudFront Functions, Lambda@Edge, or your own service. | `/cdn-cgi/image`, 5k unique/month free | Image Optimizer | Optimizer, $9.50 per site | none | none | IVM | imgproxy / thumbor |
| **Origin protection** | Secret custom origin header plus the origin-facing IP list.<br>The managed prefix list `com.amazonaws.global.cloudfront.origin-facing` only works inside AWS security groups, so a homelab has to use the list's entries or `ip-ranges.json`.<br>VPC origins only apply to origins hosted in AWS. | Tunnel, Authenticated Origin Pulls, IP ranges | header, IPs, mTLS | header, IPs | NEG plus IPs | Private Link (Azure origins) | Site Shield | n/a |
| **Real client IP** | `CloudFront-Viewer-Address` (`IP:port`; must be added in an origin request policy) | `CF-Connecting-IP` | `Fastly-Client-IP` | `CDN-ClientIP` (*verify*) | XFF | XFF / `X-Azure-ClientIP` | `True-Client-IP` | XFF |
| **Honors `CDN-Cache-Control`** | No. It uses `Cache-Control` `s-maxage`, kept within the cache policy's min/max TTL. | Yes | *verify* (honors `Surrogate-Control`) | *verify* | No | No | targeted (*verify*) | Souin: yes |
| **Honors origin `Vary`** | Only through the cache key: headers must be listed in the cache policy (*verify* the exact behavior). | Partial | Yes | Partial | Partial | Partial | Yes | Yes |
| **Stats** | CloudWatch `AWS/CloudFront` metrics, per distribution. Cache hit rate needs "additional metrics".<br>Standard logs v2 give a per-host breakdown. Real-time logs go through Kinesis. | GraphQL Analytics | real-time stats API | Statistics API | Cloud Logging | Azure Monitor | Reporting API | Prometheus |
| **IaC** | OpenTofu `aws`: distribution, cache, origin-request and response-headers policies, functions, ACM, IAM.<br>The flat-rate plan subscription is a billing action (*verify* whether the API or IaC can do it). | `cloudflare` | `fastly` | `bunnynet` (*verify*) | `google` | `azurerm` | `akamai` | Helm |
| **kof22-scale cost** (100–500 GB, ~3M requests/mo) | **Flat-rate Pro $15**: 10M requests, 50 TB, no overages, includes WAF, DDoS protection and log/S3 allowances.<br>The Free flat plan (1M requests / 100 GB) is too small.<br>Pay-as-you-go would be about $0 inside the always-free tier, but has no cost ceiling. | $0 (Free) | free tier too small; packages from $1,500 | $1–5 | ~$10–40 plus ALB | ~$35+ | contract | $0, but all traffic still goes over the home uplink |

### 2.2 Takeaways for the design

1. **CloudFront has no tags, and paths cost money once the free 1,000 per month are used.** The tag model stays provider-neutral. The CloudFront adapter adds a **path planner** that maps tags to paths and wildcards, collapses sibling paths into wildcards, and spends against a monthly budget (§4.3).
2. **CloudFront caches by its cache policy, not by the origin's `Vary` header.** For Next.js, the cache key must include the `_rsc` query parameter and the `RSC` header (§5.2, §6). `/_next/image` content negotiation needs a normalized `Accept` header.
3. **CloudFront ignores `CDN-Cache-Control`.** The qbit puts the edge TTL in `s-maxage`. The cache policy sets min TTL 0, default 0 and max 1 year, so the origin headers decide.
4. **The origin cannot rely on an IP check at Traefik while traffic is SNAT'd.** The IP filter goes at UniFi, and the secret header check goes at Traefik (§7.1).
5. **Next.js does not purge any CDN, and with 2 pods every pod must be revalidated** before the edge is invalidated. Both the Next.js self-hosting guide and its CDN caching guide say so.

### 2.3 Standards

| Standard | How the design uses it |
|---|---|
| RFC 9111 | `max-age`, `s-maxage`, `public`/`private`/`no-store`, and `immutable` (RFC 8246) |
| RFC 9213 | `CDN-Cache-Control` is emitted for providers that honor it. It is harmless on CloudFront. |
| RFC 5861 | `stale-while-revalidate` and `stale-if-error`. CloudFront serves stale content on origin errors according to its error-caching settings (*verify* how much it honors `stale-*`). |
| RFC 9110 §8.8/§13 | Weak `ETag` and `If-None-Match` → 304. CloudFront revalidates expired objects with conditional requests. |
| RFC 9211 Cache-Status | Normalized HIT/MISS. Souin emits it natively. The adapters normalize CloudFront's `X-Cache: Hit from cloudfront`. |
| Tag headers | `Cache-Tag`, `Surrogate-Key`, `CDN-Tag`, `Edge-Cache-Tag`, `xkey`. A tag header is emitted only when the provider has one. **CloudFront gets none**, because an unused tag header would leak to visitors. |

---

## 3. Architecture

```
                               ┌──────────────────────────── qbit-cdn (in each QQQ app) ─────────────────────────────┐
 CMS edit / publish ──commit──►│ ESB topic kof22.cms.tables ─► cdnEnqueuePurgeFromEvents (EsbTrigger, BATCH)         │
                               │      └─► CdnTagResolver (cdnTableTagRule) ─► cdnPurgeRequest rows (PENDING, tags)     │
                               │                                                                                       │
                               │ cdnDispatchPurges (scheduled every 5 s; flush window 30 s for CloudFront)             │
                               │   coalesce ─► [cdnRevalidationHook: every Next pod revalidateTag] ─►                  │
                               │   provider planner (CloudFront: tags→paths/wildcards within budget) ─► provider.purge │
                               │                                                                                       │
 HTTP responses ◄──────────────┤ CdnResponseHeaderPolicy (cdnCachePolicy): Cache-Control/s-maxage, ETag/304, tags*    │
 FILE_PREVIEW / Next loader ◄──┤ CdnUrlBuilder (media URL, rendition picker, signed URL later)                         │
 Admin → Health widget ◄───────┤ cdnEdgeStatDaily ◄── cdnImportEdgeStats (CloudWatch, daily)                          │
                               └───────────────────────────────────────────────────────────────────────────────────────┘
                                   adapters: CLOUDFRONT (first) · SOUIN/VARNISH (tests) · NONE · later CLOUDFLARE/FASTLY/BUNNY
                                                                  │ CreateInvalidation (IAM user cdn-qbit-kof22)
                                                                  ▼
 Visitor ─► CloudFront (1 distribution: kof22.com, www, api, s3; ACM us-east-1; flat-rate Pro)
              └─ HTTPS to origin.kof22.com:8443 + header X-Origin-Verify ─► UniFi WAN:8443 (source = CloudFront origin-facing ranges)
                   ─► Cilium LB 10.100.22.64:8443 ─► Traefik entry point "cdn" ─► forwardAuth origin-guard (secret ✓ → X-Real-IP)
                        ─► www / api / s3 services            (direct visitors keep using WAN:443 → entry point "websecure")
```

### 3.1 Where code lives (D10)

| Piece | Location |
|---|---|
| SPI, adapters, tables, processes, widget, header policy, URL builder, path planner | New repo **`QRun-IO/qbit-cdn`** (`com.kingsrook.qbits.cdn`). It follows the `qbit-template` / `qbit-quick-search` pattern: `CdnQBitConfig implements QBitConfig`, `CdnQBitProducer implements QBitMetaDataProducer<CdnQBitConfig>`, and table producers as `QBitComponentMetaDataProducer`. |
| Response header hook | QQQ core, `qqq-middleware-javalin`: `QJavalinResponseHeaderCustomizerInterface`. It is registered on `QJavalinMetaData` and called after the API and table GET handlers, with table, record and route context. It is generic, takes about 1 day, and lands **after RC.4** in the next 4.1.x. |
| Next.js helpers | `src/lib/cdn/` in Website-Frontend for v1: image loader, revalidate route, cache headers. Extract them to `@qrun-io/cdn-next` if a second app needs them. |
| Edge config | Praetor: a new `terraform/cdn-cloudfront/` root, `dns/route53` (octoDNS) and `unifi/` (port-forward and firewall group).<br>cluster-gitops: the Traefik `cdn` entry point, `origin-guard`, and middlewares. |
| `origin-guard` | A small container (nginx, or a ~50-line Go binary) in cluster-gitops `platform/origin-guard/`. Its secret comes from Infisical through ESO. |

### 3.2 Request classes and default policies

These are seeded as `cdnCachePolicy` rows (§4.4) and pinned by the header matrix (§9.3). The CloudFront cache policy uses min TTL 0, default 0 and max 31536000. That means **the origin header always decides**, and a response without a caching header is not cached.

| Class | Example | `Cache-Control` from origin | Invalidation | Notes |
|---|---|---|---|---|
| Immutable media | `s3…/kof22-website-files/media/1/x/<sha>/gallery.jpg` | `public, max-age=31536000, immutable` | Only on delete or takedown (exact paths) | The key changes when the content changes |
| Hashed build assets | `/_next/static/*` | `public, max-age=31536000, immutable` (set by Next) | Never | |
| Optimized images | `/_next/image?url=…&w=…&q=…` | `public, max-age=31536000` (with `minimumCacheTTL`, §6.1) | Never in practice, because the source is immutable | The cache key includes `url`, `w`, `q` and a normalized `Accept` |
| API leaf (single record) | `api…/v1/blog-posts/123` | `public, max-age=60, s-maxage=86400, stale-while-revalidate=60, stale-if-error=86400` | `/v1/blog-posts/123*` | Long edge TTL because it gets invalidated |
| API hub (lists, search, feeds, sitemap) | `/v1/blog-posts?blog=…`, `/v1/feeds/*`, `/v1/sitemap.xml` | `public, max-age=60, s-maxage=120, stale-while-revalidate=60, stale-if-error=86400` | **None.** It relies on a short TTL instead. | Keeps the path budget small (§4.3) |
| HTML leaf (after P4) | `/blog/quick-bits/my-post` | `public, max-age=0, must-revalidate, s-maxage=86400, stale-while-revalidate=60`. The policy sets this, overriding Next's ISR default. | `/blog/quick-bits/my-post*` (also covers the `_rsc` variants) | |
| HTML hub (after P4) | `/`, `/blog/quick-bits`, `/projects` | `public, max-age=0, must-revalidate, s-maxage=60, stale-while-revalidate=300` | None, or 1 path when published from the record action | |
| Never cache | `/v1/me*`, `/v1/contact`, `/oauth/*`, `/api/*` (frontend, including `/api/e`), `/preview*`, and all `POST` requests | `private, no-store` | n/a | The CloudFront behaviors for these paths also use the managed `CachingDisabled` policy |

---

## 4. The SPI and QQQ integration

### 4.1 `CdnProviderInterface`

```java
public interface CdnProviderInterface
{
   CdnProviderType getType();   // CLOUDFRONT, CLOUDFLARE, FASTLY, BUNNY, AKAMAI, AZURE_FRONT_DOOR, GOOGLE_CDN, SOUIN, VARNISH, NONE
   Set<CdnCapability> getCapabilities(CdnProviderConfig config);
                                // PURGE_URL, PURGE_WILDCARD, PURGE_PREFIX, PURGE_TAG, PURGE_HOST, PURGE_ALL, SOFT_PURGE,
                                // SIGNED_URL, SIGNED_COOKIE, IMAGE_TRANSFORM, STATS_IMPORT, CONFIG_READ, HONORS_CDN_CACHE_CONTROL
   CdnHeaderDialect getHeaderDialect();          // tag header (or none), separator, budget, cache-status header + parser
   CdnPurgePlanner getPurgePlanner(CdnProviderConfig config); // turns canonical tags into this provider's purge batches (§4.3)
   CdnPurgeResult purge(CdnPurgeBatch batch) throws CdnException;
   CdnPurgeStatus checkPurge(CdnProviderConfig config, String providerRequestId); // async providers (CloudFront: GetInvalidation)
   CdnHealth checkHealth(CdnProviderConfig config);
   default List<CdnEdgeStat> importStats(CdnStatsInput input)   { throw new CdnCapabilityException(STATS_IMPORT); }
   default String signUrl(CdnSignInput input)                   { throw new CdnCapabilityException(SIGNED_URL); }
   default CdnConfigSnapshot readConfig(CdnProviderConfig config) { throw new CdnCapabilityException(CONFIG_READ); }
}
```

**Image transforms** have their own SPI, `ImageTransformProviderInterface`.
- v1 ships `RENDITIONS`, which picks the nearest `thumbnail`/`gallery`/`slideshow` rendition, and `NEXT_DEFAULT`.
- `CLOUDFLARE_IMAGES`, `IMGPROXY`, `BUNNY_OPTIMIZER` and a later `CLOUDFRONT_FUNCTION` engine are reserved slots.

**Adapters are stateless.** They receive identifiers plus a **secret reference** (the name of an env var), never a secret value.
- HTTP adapters use the JDK `HttpClient`.
- The CloudFront adapter uses AWS SDK v2 `cloudfront` and `cloudwatch`, because SigV4 is not worth writing by hand. It authenticates with the key of the `cdn-qbit-kof22` IAM user (§7.5).

### 4.2 Canonical tags (provider-neutral)

A tag has the form `<kind>:<id>[:<facet>]` and is at most 64 ASCII characters. Examples: `site:1`, `post:123`, `blog:7`, `blog:7:list`, `page:12`, `media:456`, `author:3`, `project:9`, `path:/blog/quick-bits/my-post`.

- **Providers with a tag header:** tags are emitted within the portable budget of ≤ 20 tags and ≤ 1,000 bytes. A list response collapses to its list tag.
- **CloudFront:** no tag header is emitted. Tags exist only inside the qbit, as the unit of invalidation, and the planner turns them into paths.

The same tag vocabulary is used by Next.js `cacheTag()` (§6.3). A tag therefore means the same thing in Next's cache, in the CDN, and in `cdnPurgeRequest`.

### 4.3 Purge planning, and the CloudFront path planner

`CdnPurgePlanner` turns a merged group of tags into provider batches.

- **Providers with tag purge** (Fastly, Bunny, Cloudflare, Souin) send the tags as-is, in batches sized to the provider's `itemsPerRequest`.
- **CloudFront** uses `CloudFrontPathPlanner`, which works in five steps:
  1. **Expand.** Each tag maps to invalidation paths through `cdnTableTagRule.invalidationPaths`. The paths are rendered from the event's `record` **and** its `oldRecord`, so a slug change also clears the old URL.
     - Example: `post:123` becomes `www:/blog/{blog.slug}/{slug}*` and `api:/v1/blog-posts/{id}*`.
     - The trailing `*` covers the RSC `?_rsc=` variants and other query variants.
     - The `host:` prefix is dropped, because one distribution serves all hosts and paths are host-agnostic. The trade-off is that `/v1/blog-posts/123*` on `api` and an identical path on `www` are invalidated together, which is harmless.
  2. **Skip hubs.** Paths whose policy class is *hub* (§3.2) are dropped, because they expire on their own within 60–120 s. A manual "Purge from CDN" action can include them on purpose.
  3. **Collapse.** If more than `collapseThreshold` paths (default 5) share a parent, they are replaced by `parent/*`, which costs 1 path. Any path already covered by a wildcard in the same batch is removed.
  4. **Budget.** The planner counts this month's billed paths as the sum of `cdnPurgeRequest.billablePaths` for SUCCEEDED requests in the current UTC month. CloudFront bills a wildcard as 1 path and an exact path as 1 path.
     - **Below the soft limit** (default 800 of the free 1,000): send as planned.
     - **Between the soft and hard limits** (hard default 1,000): coarsen to one wildcard per top-level section, such as `/blog/*` or `/v1/*`.
     - **Above the hard limit:** either pay ($0.005 per path, up to `monthlyPaidPathCap`, default 1,000 paths or $5), or queue the request as `DEFERRED_BUDGET` and let the TTL expire it. Each provider chooses one, and the default is to pay.

     The widget always shows the month-to-date count against the budget.
  5. **Respect concurrency.** At most `maxInFlightWildcards` wildcards are in flight (default 10, under CloudFront's in-progress wildcard quota), and only one `CreateInvalidation` per distribution. The next one is sent after `GetInvalidation` reports `Completed`, or after 15 minutes.
     - Each `CreateInvalidation` uses the `cdnPurgeRequest` id as its `CallerReference`, so an at-least-once retry never creates two invalidations.
- **Never automatic:** `/*` (purge everything). It is only reachable through the guarded "Purge hostname" action, which requires `cdn.purgeAll`. With a single distribution, on CloudFront that means the whole distribution, and the confirmation screen says so.

A typical kof22 publish costs **2 billable paths**: the post page wildcard and the API record wildcard. The free 1,000 paths cover about **500 publishes a month**, far more than kof22 needs.

### 4.4 QQQ tables

These are normal, visible QQQ tables. Each has the `cdn` prefix, audit enabled, and `id`/`createDate`/`modifyDate`.

| Table | Purpose | Key fields |
|---|---|---|
| **`cdnProvider`** | One row per CDN or image engine | `name`, `providerType`, `role` (CDN / IMAGE_TRANSFORM), `enabled`, `accountRef` (for example AWS account `744245396565`), `zoneOrDistributionId`, `secretRef` (env var name only), `settings` (JSON: `softPathLimit`, `hardPathLimit`, `monthlyPaidPathCap`, `overBudgetMode` PAY/DEFER, `flushWindowSeconds`, `collapseThreshold`), `lastHealthStatus`, `lastHealthCheckDate`, `capabilities` (read-only display) |
| **`cdnHostname`** | Hostnames and their rollout stage | `hostname`, `cdnProviderId`, `hostnameRole` (SITE / API / MEDIA / ASSETS), `stage` (OFF / HEADERS_ONLY / LIVE / LOCKED), `purgeEnabled`, `notes` |
| **`cdnCachePolicy`** | Header policy per hostname and path, or per route class | `name`, `cdnHostnameId`, `matchType` (PREFIX / GLOB / ROUTE_CLASS / TABLE), `matchValue`, `tableName`, `policyClass` (IMMUTABLE / LEAF / HUB / NEVER), `browserMaxAge`, `edgeMaxAge`, `staleWhileRevalidate`, `staleIfError`, `immutable`, `noStore`, `varyHeaders`, `priority`, `enabled`, `seeded` |
| **`cdnTableTagRule`** | How a table's changes become tags and paths | `tableName`, `tagTemplates`, `invalidationPaths` (templates, with optional `host:` prefix and trailing `*`), `onlyWhenFieldsChange`, `publishedFilter`, `enabled` |
| **`cdnPurgeRequest`** | Every purge | `cdnProviderId`, `kind` (TAG / PATHS / HOST / ALL), `tags`, `paths` (as planned), `billablePaths`, `source` (ESB_EVENT / RECORD_ACTION / MANUAL / DEPLOY / RETRY), `sourceTable`, `sourceRecordId`, `causationId`, `requestedByUserId`, `status` (PENDING / MERGED / PLANNED / SENT / IN_PROGRESS / SUCCEEDED / FAILED / DEAD / DEFERRED_BUDGET / SKIPPED_STAGE_OFF / SKIPPED_NONE_PROVIDER / SKIPPED_HUB_ONLY), `mergedIntoId`, `attempts`, `nextAttemptDate`, `providerRequestId` (the CloudFront invalidation id), `httpStatus`, `errorMessage`, `sentDate`, `completedDate` |
| **`cdnRevalidationHook`** | Caches to refresh before the CDN (the Next pods) | `name`, `url`, `fanOutDns` (resolve every pod behind a headless Service), `secretRef`, `timeoutMs`, `enabled` |
| **`cdnEdgeStatDaily`** | Imported stats (real data only) | `cdnProviderId`, `scope` (DISTRIBUTION, or a hostname when logs give a per-host breakdown), `statDate`, `requests`, `bytesDownloaded`, `cacheHitRate` (null unless CloudFront additional metrics are on), `error4xxRate`, `error5xxRate`, `importedDate`. Unique on (provider, scope, date). |

### 4.5 Event pipeline

**`cdnEnqueuePurgeFromEvents`** is an `EsbTrigger` on the app's CMS topic (for kof22, `kof22.cms.tables`). It runs in `BATCH` mode with concurrency 1, `maxAttempts` 5 and the standard DLQ. For each CloudEvent `qqq.table.<t>.inserted|updated|deleted`, it:
1. finds the table's `cdnTableTagRule`
2. applies `publishedFilter` and `onlyWhenFieldsChange`, so a draft autosave of `blog_post_version` invalidates nothing
3. renders the tags from `record` and `oldRecord`
4. inserts one `cdnPurgeRequest` (PENDING) for each provider whose affected hostnames are LIVE or LOCKED.

If no such hostname exists, it records the request as `SKIPPED_STAGE_OFF`. During HEADERS_ONLY this shows what would have been purged.

**`cdnDispatchPurges`** is scheduled every 5 s, with a single runner under the scheduler's clustered lock. For each provider, it:
1. **Waits out the flush window.** For CloudFront the default is 30 s from the oldest PENDING row, because invalidations are slow and billed per path, so batching pays.
2. **Merges** the PENDING rows. The merged rows become `MERGED`, with `mergedIntoId` set.
3. **Runs the revalidation hooks first.** Every Next pod runs `revalidateTag(tags)` (§6.3), and every pod must return 200. If any fails, the dispatcher retries the hook and does not invalidate the edge yet. Invalidating the edge while a pod still serves stale HTML would just cache the stale page again.
4. **Plans** the batch with the provider planner (§4.3) and **sends** it.
5. **Tracks async status.** `IN_PROGRESS` rows are polled through `checkPurge` (CloudFront `GetInvalidation`) until they reach `SUCCEEDED`.
6. **Retries** with backoff of 5 s, 30 s, 2 min, 10 min and 1 h. After 5 failed attempts the row is `DEAD` (D14).

Apps without the ESB call `CdnPurgeService.enqueue(tags, source)` from an after-commit callback.

### 4.6 Response headers and media objects

`CdnResponseHeaderPolicy` finds the best-matching enabled `cdnCachePolicy` and writes:
- `Cache-Control` (browser `max-age`, edge `s-maxage`, `stale-*`, `immutable`)
- `CDN-Cache-Control`, only if the provider honors it
- the tag header, only if the provider has one
- a weak `ETag`, answering `If-None-Match` with 304.

**Safety rule:** if the response has `Set-Cookie`, the request carries `Authorization`, or the session is authenticated, the policy forces `private, no-store`, whatever the policy row says.

**Wiring.**
- QQQ API and table GETs go through the new core hook.
- kof22's `SiteApiRouteProvider.finish()` calls `CdnResponseHeaderPolicy.apply(...)` instead of its hard-coded `max-age` values. The seeded policies reproduce today's values and add `s-maxage`.

**MinIO objects.**
- (a) Uploads set `Cache-Control: public, max-age=31536000, immutable` (in `PhotoImageProcessor.uploadToS3` and `MediaImageProcessor`).
- (b) A one-time backfill, `cdnBackfillObjectHeaders`, fixes existing keys with a copy-in-place using REPLACE metadata. It runs as a dry run first.
- (c) As a fallback, the CloudFront behavior for `/kof22-website-files/*` uses a cache policy with a **default TTL of 1 year**, so objects that still lack headers are cached anyway.

### 4.7 Processes (table and record actions only)

| Process | Where it appears | Permission | What it does |
|---|---|---|---|
| `cdnPurgeRecordUrls` | Record action **"Purge from CDN"** on tables with an enabled `cdnTableTagRule`; also a bulk action | `cdn.purge` | A review screen shows the tags, the planned paths (hub paths optional), and the **billable path count against the month-to-date budget**. Then it enqueues the purge and shows the result. |
| `cdnWarmRecordUrls` | Record action **"Warm CDN cache"** | `cdn.purge` | Fetches the record's public URLs (HTML and `_rsc`) through the CDN with concurrency 2, and reports `X-Cache` for each |
| `cdnRetryPurge` | Record action on `cdnPurgeRequest` rows that are FAILED, DEAD or DEFERRED_BUDGET | `cdn.purge` | Requeues the request |
| `cdnPurgeHostname` | Record action on `cdnHostname` | `cdn.purgeAll` | Guarded: the user must type the hostname to confirm. On CloudFront it sends `/*` for the **whole distribution**, and the screen says so. |
| `cdnTestProvider` | Record action on `cdnProvider` | `cdn.admin` | Checks credentials, that the distribution exists, and the IAM scope (with `ListInvalidations`). Confirms the origin custom header is configured, using a read-only `GetDistributionConfig`. |
| `cdnImportEdgeStats` | Table action on `cdnEdgeStatDaily`; also scheduled daily at 02:15 | `cdn.admin` | Imports the last N days of CloudWatch metrics, as an upsert |
| `cdnBackfillObjectHeaders` | Table action on `cdnHostname` (MEDIA role) | `cdn.admin` | Runs the MinIO metadata backfill (§4.6) |

### 4.8 Permissions and locks

- Permissions are `cdn.view`, `cdn.purge`, `cdn.purgeAll` and `cdn.admin`.
- `cdnPurgeRequest` is written only by processes, and all of its fields are read-only in the UI.
- No table stores a secret value.
- Multi-tenant apps add the tenant key and a `RecordSecurityLock` to every `cdn*` table.

### 4.9 Admin → Health: CDN widget

The widget shows real data only. When a section has no data, it says why. For example: "CloudFront cache hit rate not available: additional metrics are off for this distribution."

It shows:
- **Per provider:** 7-day requests, bytes and 4xx/5xx rates from `cdnEdgeStatDaily`, the cache hit rate if it was imported, and a daily sparkline.
- **Invalidation budget:** month-to-date billable paths against the soft limit, the hard limit and the free 1,000, plus a projection to month end at the current rate.
- **Pipeline:** the PENDING count and the age of the oldest row, the IN_PROGRESS count, and SUCCEEDED/FAILED/DEAD/DEFERRED counts for the last 24 h. Each count links to a filtered `cdnPurgeRequest` list.
- **Provider health:** the last `cdnTestProvider` result.

Below 640 px wide, the widget becomes a single column of label/value rows.

Data Health rules:
- `operations.cdn-dead-purges`
- `operations.cdn-purge-backlog` (the oldest PENDING row is older than 5 min)
- `operations.cdn-budget` (above the soft limit)
- `operations.cdn-stats-stale` (no import in 48 h)

### 4.10 URL building and signing

- **`CdnUrlBuilder.mediaUrl(key)`** is unchanged: `https://s3.kof22.com/kof22-website-files/<key>`. Once that hostname is LIVE, CloudFront serves it, so **no stored URL changes**.
- **`imageUrl(source, w, …)`** uses `RENDITIONS` for the admin `FILE_PREVIEW` thumbnails and for the Site API `srcset` hints. The frontend uses Next's optimizer (§6.2).
- **`signedUrl(key, ttl)`** is not used in v1 (D12). Private and draft media use MinIO presigned GETs on the **origin-only upload hostname** (§5.1), which skips the edge.

Later, CloudFront **trusted key groups** can provide signed URLs and cookies. A public key is registered in the distribution and the private key is kept in Infisical. The adapter's `signUrl` would implement the canned-policy signature, which is about 1–2 days of work when it is needed.

---

## 5. kof22 specifics

### 5.1 Hostnames

| Hostname | Role | Notes |
|---|---|---|
| `s3.kof22.com` | MEDIA | Only `/kof22-website-files/*` is cached. Everything else on the host (other buckets, presigned `X-Amz-*` requests, writes) gets `CachingDisabled` and `AllViewer` forwarding.<br>**Before cutover**, presigned uploads move to a new DNS-only hostname, **`upload.kof22.com`** (`SITE_API_S3_PUBLIC_ENDPOINT`), with an IngressRoute to MinIO on `websecure`. That way big PUTs and SigV4 Host signing never go through CloudFront. |
| `www.kof22.com`, `kof22.com` | SITE + ASSETS | Stage A: static assets, `/_next/image` and `public/` files. Stage B: HTML (P4).<br>The apex keeps its current redirect behavior and is served through the same distribution, as an ALIAS. |
| `api.kof22.com` | API | Policies from §3.2. `/v1/me*`, `/v1/contact`, `/oauth/*` and all writes are `CachingDisabled`. |
| `admin`, `auth`, `minio-console`, DSM suite | none | Never proxied (D4) |
| `origin.kof22.com` (new) | origin | DNS-only A record to `50.122.5.146`. Used only as CloudFront's origin domain, on port 8443. |
| `upload.kof22.com` (new) | origin-only | DNS-only. Serves presigned uploads and private presigned GETs. |

### 5.2 CloudFront behaviors and cache keys (one distribution)

There is **one distribution**, with the aliases `kof22.com`, `www.kof22.com`, `api.kof22.com` and `s3.kof22.com`.
- **The origin receives the viewer's `Host`**, so Traefik routes exactly as it does today.
- Every cache policy includes `Host` in the key, so `www`, `api` and `s3` never share objects.
- CloudFront validates the origin certificate against the forwarded Host, and the LE wildcard covers all of them.

| Behavior (path pattern) | Cache policy (key) | Origin request policy | Notes |
|---|---|---|---|
| `/_next/static/*` | `Host` only; no query strings, no cookies | minimal | immutable |
| `/_next/image*` | `Host`; query strings `url`, `w`, `q`; header `Accept`, **normalized** by a viewer-request CloudFront Function to `image/avif`, `image/webp` or `*/*` | minimal plus `Accept` | Prevents format poisoning. CloudFront Functions are part of the flat-rate plan's edge compute allowance (*verify*). |
| `/kof22-website-files/*` | `Host` only, default TTL 1 year | minimal | media |
| `/api/*`, `/preview*`, `/v1/me*`, `/v1/contact*`, `/oauth/*` | `CachingDisabled` (managed) | `AllViewer` plus `CloudFront-Viewer-Address` | never cached |
| `*` (default) | `Host`; a query-string **allow-list** (everything except `utm_*`, `gclid`, `fbclid` and `ref`), which includes `_rsc`; header `RSC` (separates Next RSC from HTML); no cookies | A custom policy, **not** `AllViewerExceptHostHeader`. It forwards all query strings, plus `Host`, `RSC`, `Next-Router-State-Tree`, `Next-Router-Prefetch`, `Next-Router-Segment-Prefetch`, `Next-Url` and `CloudFront-Viewer-Address`. | Next derives the `_rsc` value from the router headers, so `_rsc` plus `RSC` in the key is enough (*verify* against the Next CDN guide for 16.x) |

Other distribution settings:
- Cached behaviors allow `GET`, `HEAD` and `OPTIONS`. The `CachingDisabled` behaviors allow all methods.
- HTTP/2 and HTTP/3 are on, and so is compression.
- Price class is the flat-rate plan's default.

### 5.3 Tag and path rules (seeded `cdnTableTagRule`)

| Table | Tags | Invalidation paths (leaves only; hubs expire by TTL) |
|---|---|---|
| `blog_post`, `blog_post_version` | `post:{id}`, `blog:{blogId}:list`, `site:{siteId}:feeds` | `/blog/{blog.slug}/{slug}*`, `/v1/blog-posts/{id}*` |
| `blog` | `blog:{id}` | `/v1/blogs/{id}*` (the blog index page is a hub) |
| `page`, `page_section` | `page:{pageId}` | `/{page.slug}*`, `/v1/pages/{pageId}*` |
| `author` | `author:{id}` | `/v1/authors/{id}*`. Pages that embed the author card expire as hubs, or through a manual purge. |
| `media`, `photo`, `photo_album` | `media:{id}`, `album:{albumId}` | On delete or takedown only: the 4 rendition keys, as exact paths |
| `project` (+section/update) | `project:{id}` | `/projects/{slug}*`, `/v1/projects/{id}*` |
| `menu`, `menu_item`, `site`, `domain`, `snippet`, `redirect` | `site:{siteId}` | None by default. These appear on every page, so a change waits for the HTML leaf TTL. The rule offers an optional `/*` that **only an admin action can trigger**. |

The HTML leaf `s-maxage` (1 day) is the longest a menu or snippet change can take to appear without a manual "Purge hostname". If the owner wants faster, the leaf TTL can drop to 1 h. It is one seeded policy value.

### 5.4 Site API changes (Website-Backend)

1. `finish()` uses `CdnResponseHeaderPolicy` instead of hard-coded values.
2. Anonymous reads get a weak ETag and answer 304.
3. Anonymous GETs drop `Vary: Origin` and send `Access-Control-Allow-Origin: *` instead, since those responses are public, cookie-less and published-only. Credentialed routes keep the allow-list, and they are `no-store`.
4. The Traefik rate limiters change from `requestHost` to `ipStrategy`, keyed on `X-Real-IP`. The guard sets that header once a hostname is behind CloudFront (§7.3).

---

## 6. Next.js frontend integration

### 6.1 Stage A (P3): no rendering change

- `headers()`: `public/` files get `max-age=86400, s-maxage=604800`, and `/api/*` gets `no-store`.
- `images.minimumCacheTTL = 31536000`, because the image sources are immutable.
- `deploymentId` for version-skew protection.
- `/_next/static` is already immutable.

### 6.2 Images (D7)

v1 keeps the built-in `/_next/image` optimizer, cached at the edge (§5.2), plus the existing renditions. No component changes. The `Accept`-normalizing CloudFront Function is the only code running at the edge.

Later and optional: a `CLOUDFRONT_FUNCTION` or Lambda@Edge resize engine behind `ImageTransformProviderInterface`, used through a custom `loaderFile`. Shared URL fixtures keep the backend and frontend in sync (§9.1).

### 6.3 Stage B: HTML caching (P4, D8)

1. Remove `force-dynamic` from the root layout. Keep it only on `/preview` and on routes that personalize.
2. **ISR.** Data fetches use `cacheTag()` / `fetch(..., { next: { tags } })` with the canonical tags (`post:123`, `blog:7:list`, `site:1`). Pages also set a time-based `revalidate` as a safety net.
3. A `headers()` rule, or the policy in `src/proxy.ts`, sets `Cache-Control` for HTML leaves and hubs (§3.2). This overrides Next's default ISR header so browsers always revalidate while the edge holds the page.
   - *Verify* that Next 16 lets `headers()` override the ISR `Cache-Control`.
   - If it does not, set the min TTL in the CloudFront cache policy for the HTML behaviors, and keep the browser TTL with a response headers policy.
4. `POST /api/revalidate` takes `{tags}` with an HMAC made from `CDN_REVALIDATE_SECRET`. It calls `revalidateTag` for each tag and returns 200 once done.
5. **Every pod, then the edge.** The `cdnRevalidationHook` with `fanOutDns` resolves the headless Service `website-frontend-headless` and POSTs to **each** pod. The dispatcher creates the CloudFront invalidation only after every pod has returned 200 (§4.5). This avoids the stale-pod problem the Next self-hosting guide warns about, without needing Redis.
6. Optionally, the "Warm CDN cache" action then fetches the HTML and `_rsc` variants.

---

## 7. Security and privacy

### 7.1 Origin protection on CloudFront (D3)

CloudFront has no tunnel. VPC origins only reach origins inside an AWS VPC, so using them would mean an AWS-side proxy plus a VPN into the homelab, which adds cost and a new failure point. That option is **rejected**.

The design instead layers three independent controls:

1. **Network layer: UniFi, as IaC in Praetor `unifi/`.**
   - A **new** port-forward, WAN `8443` → `10.100.22.64:8443`, restricted to a source firewall group of CloudFront's **origin-facing** IPv4 ranges.
   - The group's ranges come from the entries of the managed prefix list `com.amazonaws.global.cloudfront.origin-facing`, read with the OpenTofu data source `aws_ec2_managed_prefix_list` in `terraform/cdn-cloudfront`. The alternative source is the `CLOUDFRONT_ORIGIN_FACING` service in `ip-ranges.json`.
   - `make cdn-origin-ranges` regenerates the group. A weekly CI check compares the `syncToken` in `ip-ranges.json` with the last applied one, and opens a PR when it changes.
   - The existing `443` forward is unchanged. It still carries direct traffic and the other tenants.
   - *Verify* that a UniFi firewall group can hold the prefix list's entry count (about 55).
2. **Application layer: the Traefik `cdn` entry point on `:8443`.**
   - Every route on this entry point goes through a **forwardAuth** middleware to `origin-guard`.
   - The guard compares `X-Origin-Verify` with the current **or** the previous secret, so rotation causes no downtime. It returns 403 otherwise.
   - CloudFront adds the header as an **origin custom header**, configured in tofu with the value from Infisical. Viewers can neither set it nor see it.
3. **Certificate layer.** The origin presents the LE wildcard. CloudFront talks to the origin over HTTPS only, at TLSv1.2 or later.

**Why the IP check is not at Traefik.** With `externalTrafficPolicy: Cluster`, Traefik sees SNAT addresses, so an `ipAllowList` would treat every request the same way. Once the DaemonSet / `Local` change lands (D5), a Traefik `ipAllowList` on the `cdn` entry point becomes a fourth, redundant layer.

**Trade-offs against a tunnel:**

| | CloudFront: allowlisted port + secret header | Tunnel (Cloudflare, not chosen) |
|---|---|---|
| Inbound exposure | One extra WAN port, open only to CloudFront ranges | None |
| Upkeep | The CloudFront range list changes, so the CI drift check and regeneration are needed | None |
| Blast radius of one compromise | A leaked header alone is not enough, because the IP filter still applies. A request from CloudFront IPs **through another CloudFront customer's distribution** also needs the secret. | The tunnel token |
| Real-IP trust | Conditional on the secret check (§7.3) | Implicit, because only the tunnel can reach the origin |
| Single point of failure | No new component in the path except the guard, which runs 2 replicas | `cloudflared` |

### 7.2 LOCKED stage

After a hostname has been stable on CloudFront for 2 weeks, its IngressRoute drops `websecure` and keeps only `cdn`. Direct requests to `50.122.5.146:443` with that Host header then get a 404. This is reversible in gitops.

The origin IP stays discoverable, because mail, DSM and the other tenants share it. The design does not depend on hiding it.

### 7.3 Real client IP (D5)

- On the `cdn` entry point, `origin-guard` parses `CloudFront-Viewer-Address`, which has the form `ip:port`. It splits the port off at the **last** colon, so IPv6 addresses work (*verify* the IPv6 format).
- **Only when the secret is valid** does the guard return `X-Real-IP: <ip>` and `X-Edge-Verified: cloudfront`. Traefik copies both through forwardAuth `authResponseHeaders`, which **overwrite** any value the client sent.
- On `websecure`, a `headers` middleware **removes** `X-Real-IP`, `X-Edge-Verified` and `CloudFront-Viewer-Address` from incoming requests, so they cannot be spoofed through the direct path.
- **Consumers use one rule.** `IpAddresses.clientIp` (analytics), `siteapi/http/ClientIp` and the frontend's `src/lib/analytics/forward.ts` use `X-Real-IP` when `X-Edge-Verified: cloudfront` is present. Otherwise they keep today's XFF logic.
  - The frontend forwards both headers to the backend collector.
  - The backend trusts them only from in-cluster callers that hold `ANALYTICS_COLLECT_TOKEN`.
- **Result:** `GeoStatus.PROXY_IP` disappears for every hostname behind CloudFront.

The paused **Traefik DaemonSet with `externalTrafficPolicy: Local` is still wanted**. It covers hostnames that are not behind the CDN (other tenants, `admin`, `auth`), and it adds a defense-in-depth layer.

### 7.4 Privacy (D13)

- AWS becomes a processor of visitor IPs and user agents. Update the privacy page and rely on the AWS DPA. The owner must confirm which entity's AWS agreement applies (§12.2).
- CloudFront injects no scripts. Do not enable CloudWatch RUM or any client-side AWS SDK.
- Standard logs v2 contain IPs, so they stay **off** by default. If they are turned on for per-host stats (P5), use a short CloudWatch Logs retention (7 days), and have the importer read aggregates only.
- The analytics beacon `/api/e` is `CachingDisabled`, so cache hits do not undercount visits.
- IPs stay hashed with the daily salt and are never stored.

### 7.5 Credentials (following the Praetor Key A/B/C pattern)

- **Key D: IAM user `cdn-qbit-kof22`**, defined in `terraform/cdn-cloudfront/iam.tf`.
  - It is allowed `cloudfront:CreateInvalidation`, `GetInvalidation`, `ListInvalidations` and `GetDistributionConfig` on **the one distribution ARN**, plus `cloudwatch:GetMetricData` and `ListMetrics` (`Resource: *`, read-only).
  - The key lives in Infisical and reaches Website-Backend through ESO as `CDN_AWS_ACCESS_KEY_ID` and `CDN_AWS_SECRET_ACCESS_KEY`. `cdnProvider.secretRef` holds these env var names.
- **The origin secret** (`X-Origin-Verify`) lives in Infisical.
  - Tofu reads it to set the CloudFront origin custom header, and ESO reads it for `origin-guard`.
  - It rotates every 90 days, with two valid values during the overlap.
- **The app cannot change the distribution.** Only `kingsrook_root_admin`, through tofu, can.

---

## 8. Infrastructure and rollout

### 8.1 DNS and certificates (D2)

1. **ACM certificate** in `us-east-1`: one public certificate for `kof22.com` and `*.kof22.com`, validated by DNS.
   - The validation CNAME is **added to `dns/route53/zones/kof22.com.yaml`**. octoDNS owns every record in the zone and would otherwise delete it.
   - This is a two-step apply: tofu creates the certificate, its output goes into an octoDNS PR, and then tofu waits for validation.
2. **CAA:** add `0 issue "amazon.com"` (and `amazontrust.com`, if wanted) next to `letsencrypt.org`.
3. **Records**, through octoDNS, with one PR per hostname at cutover:
   - The apex `kof22.com` becomes a Route53 **ALIAS** to the distribution, using the `octodns-route53` record type `Route53Provider/ALIAS`. *Verify* the syntax for CloudFront targets (hosted zone `Z2FDTNDATAQYW2`).
   - `www`, `api` and `s3` become ALIAS records (preferred) or CNAMEs to `dxxxx.cloudfront.net`.
   - `origin` and `upload` are new A records to `50.122.5.146`.
4. **DNSSEC** is unchanged. ALIAS records are signed like any other record.
5. **cert-manager** is unchanged. The origin keeps the LE wildcard.

### 8.2 OpenTofu root `terraform/cdn-cloudfront/` (D9; nothing has been created)

**Provider block**, copied from `dns-route53`:
- `profile = "kingsrook_root_admin"`, `region = "us-east-1"`, `allowed_account_ids = ["744245396565"]`
- local git-crypt state
- `default_tags { tenant = "kof22", stack = "cdn" }` for cost allocation.

**Resources:**
- `aws_acm_certificate`, plus `aws_acm_certificate_validation`.
- `aws_cloudfront_distribution`: the aliases, one custom origin `origin.kof22.com` (`https-only`, port 8443, origin custom header from a variable), and the behaviors from §5.2.
- Four `aws_cloudfront_cache_policy` and two `aws_cloudfront_origin_request_policy` resources.
- `aws_cloudfront_response_headers_policy`: removes `Server` and `X-Powered-By`, and adds HSTS consistent with the origin.
- `aws_cloudfront_function`: the `Accept` normalizer, and stripping of tracking parameters.
- `aws_iam_user`, `aws_iam_user_policy` and `aws_iam_access_key` for Key D.
- `data.aws_ec2_managed_prefix_list` for the origin-facing ranges, with its output going to the UniFi root.

**Flat-rate Pro plan.** Subscribing is a billing action the owner takes in the console. *Verify* two things: whether the plan attaches per distribution, and whether tofu or the API can manage the attachment. If it is per distribution, the single-distribution design means **one $15 plan covers every kof22 hostname**.

**WAF.** The flat-rate plan bundles WAF. Run the AWS managed core rule set in count mode first, then switch to block. Add rate-based rules for `/v1/search` and `/v1/contact`.

**Drift.** The qbit's `cdnTestProvider` reads `GetDistributionConfig` and flags anything that differs from what is expected, such as a missing origin header, a changed behavior, or a caching policy on `/v1/me*`.

### 8.3 Cluster and network changes (all reversible)

- **Praetor `unifi/`:** a firewall group `cloudfront-origin-facing` (generated), and a port-forward WAN `8443` → `10.100.22.64:8443` limited to that group.
- **cluster-gitops `platform/traefik`:**
  - A new entry point, `cdn`, on `:8443`. It is exposed on the LoadBalancer Service as port 8443 (`10.100.22.64`), with `forwardedHeaders.trustedIPs` set to the pod CIDR `172.24.0.0/16` only.
  - New middlewares: `origin-guard-auth` (forwardAuth, with `authResponseHeaders: [X-Real-IP, X-Edge-Verified]`), and `strip-edge-headers`, added to the `websecure` chain for kof22 routes.
- **`platform/origin-guard`:** 2 replicas, a PDB, a tiny image, and its secret through ESO.
- **tenants/kof22:**
  - IngressRoutes for `www`, `api` and `s3` add `entryPoints: [websecure, cdn]`, with the guard middleware on `cdn`.
  - A new `upload.kof22.com` IngressRoute.
  - Env vars: `SITE_API_S3_PUBLIC_ENDPOINT=https://upload.kof22.com`, `CDN_*` and `CDN_REVALIDATE_SECRET`.
  - A headless Service for the frontend.

### 8.4 Staged cutover per hostname (D4)

1. **HEADERS_ONLY.** The policies emit headers, and the pipeline records `SKIPPED_STAGE_OFF` rows.
   - The header matrix passes at the origin.
   - The distribution exists with the alias attached. It is tested through the `dxxxx.cloudfront.net` name with a `Host` override, or with `curl --resolve`.
   - The DNS TTL is lowered to 60 a day ahead.
2. **LIVE.** The octoDNS PR switches the record to the ALIAS. For the next 48 h, watch:
   - the `X-Cache` hit share in the header matrix
   - CloudWatch 5xx
   - the invalidation pipeline
   - that analytics shows real geo.
3. **LOCKED.** After 2 weeks, drop `websecure` from that IngressRoute.

**Order:** `s3` (once `upload.kof22.com` is live) → `www` Stage A (with the apex) → `api` → `www` Stage B (HTML, P4).

**Rollback:**
- **From LIVE:** an octoDNS PR restores the A record to `50.122.5.146`. It takes effect in about a minute plus the apply time. Then set `cdnHostname.stage=OFF`.
- **From LOCKED:** first restore `websecure` on the IngressRoute, then roll back as from LIVE.
- **Emergency:** the A-record values are kept in a commented block in the zone file, so the revert is a one-line change.

### 8.5 Other tenants (D15)

Out of scope for now. The pattern can be reused later: the same distribution style, an IAM user like Key D, and the same UniFi port and guard. Each QQQ tenant would install the qbit with its own provider row.

---

## 9. Testing

### 9.1 Contract tests (CI)

**`CdnProviderContractTest`** is an abstract suite that each adapter extends. It runs against **WireMock** stubs built from recorded, scrubbed fixtures stored in `src/test/resources/fixtures/<provider>/`.

**CloudFront contract tests cover:**
- the `CreateInvalidation` XML shape, and idempotency through `CallerReference`
- `TooManyInvalidationsInProgress` → backoff
- `AccessDenied` → DEAD
- `GetInvalidation` polling until `Completed`
- `GetMetricData` parsing
- the `GetDistributionConfig` drift checks.

**`CloudFrontPathPlannerTest`** is table-driven. It covers:
- expansion from both `record` and `oldRecord` (slug change)
- hub skipping
- collapsing siblings into a wildcard
- removing paths a wildcard already covers
- every budget band (below soft; between soft and hard; above hard, with PAY and with DEFER)
- the month rollover
- the in-flight wildcard cap.

**Shared image URL fixtures** (`fixtures/image-urls.json`) are used by the backend `RENDITIONS` engine and by any future loader.

**Live smoke test** (`-Dcdn.live=true`, run by hand, never in CI): one invalidation of the sentinel path `/__cdn-smoke/<uuid>`, plus a stats import for yesterday.

### 9.2 Real cache end-to-end (CI, Testcontainers)

- **Souin** runs in front of a test QQQ app with the qbit and the SOUIN adapter. The test goes:
  1. MISS, then HIT
  2. update a record through a QQQ action
  3. the ESB event passes through the embedded Artemis test broker to the dispatcher
  4. MISS with the new content.
- A fake revalidation hook proves the ordering: the hook is called before the purge, and a hook failure blocks the purge.
- Safety tests:
  - a response with `Set-Cookie`, or an authenticated response, is never cacheable
  - a draft autosave enqueues nothing
  - a slug change purges both the old and the new path.
- The CloudFront path planner also runs against Souin in "paths mode", which exercises the same pipeline without AWS.

### 9.3 Header and edge matrix

`scripts/cdn-header-matrix.ts` is table-driven. It checks:
- **At the origin**, in CI against staging: `Cache-Control`, `ETag`/304, and the absence of `Set-Cookie`, for every class in §3.2.
- **At the edge, after cutover:**
  - the second request is an `X-Cache` hit for every cached class
  - there is never a hit on `/v1/me`, `/api/e`, `/preview` or `admin.kof22.com`
  - `/_next/image` returns a different `Content-Type` for each normalized `Accept`.
- **Origin protection:**
  - a direct request to `origin.kof22.com:8443` without the secret header gets **403** from the guard
  - a spoofed `X-Real-IP` sent on `websecure` never reaches the app.

---

## 10. Observability and cost

### 10.1 Observability

- **Tables** feed the widget and Data Health (§4.9).
- **Micrometer metrics**, when the app has a registry:
  - `cdn_purge_requests_total{provider,status}`
  - `cdn_purge_latency_seconds` (enqueue → SUCCEEDED, including CloudFront propagation)
  - `cdn_invalidation_paths_month`
  - `cdn_purge_pending`
- **PrometheusRules:**
  - `CdnPurgeDead`
  - `CdnPurgeBacklog` (over 10 min)
  - `CdnInvalidationBudgetSoft`
  - `OriginGuardDown`: 0 ready replicas means every LIVE hostname returns 5xx through CloudFront
  - `OriginGuardRejectSpike`: a jump in the 403 rate, which may mean secret drift after a rotation
- **CloudWatch alarm** (via tofu, optional): distribution `5xxErrorRate > 5%` for 10 min, sent to SNS email.

### 10.2 Cost (kof22: 100–500 GB and ~3M requests a month)

| Item | Monthly |
|---|---|
| CloudFront flat-rate **Pro**: 10M requests, 50 TB, WAF, DDoS protection, edge compute and log allowances, no overages | **$15** |
| Invalidations: about 2 paths per publish, well under the free 1,000 | $0. Capped by `monthlyPaidPathCap` at 1,000 paid paths, which is $5. |
| ACM public certificate, Route53 ALIAS queries | $0 |
| CloudWatch `GetMetricData` (a few hundred metric reads per day) | < $0.10 |
| CloudFront additional metrics (cache hit rate), *optional* | About $0.30 per metric per month on pay-as-you-go. *Verify* whether flat-rate includes it. |
| Standard logs v2 to CloudWatch Logs, *optional, off by default* | Within the plan's log allowance (*verify*), with 7-day retention |
| `origin-guard` pods, UniFi | $0 |
| **Expected total** | **About $15–20/mo**, within D11's $0–25 |

---

## 11. Phased plan (CloudFront)

Estimates assume one engineer working with Claude. Each phase can be deployed on its own.

| Phase | Scope | Estimate | Exit criteria |
|---|---|---|---|
| **P0: origin hygiene** (no CDN) | MinIO `Cache-Control` on upload, plus the backfill. Site API ETag/304 and the `Vary: Origin` cleanup. Next `headers()`, `minimumCacheTTL` and `deploymentId`. The header matrix script. **The `upload.kof22.com` split.** | 3–4 d | The matrix passes in production, and presigned uploads work on `upload.kof22.com`. |
| **P1: qbit core** | `QRun-IO/qbit-cdn`: the SPI, the NONE and SOUIN adapters, the 7 tables, the header policy, the ESB trigger, the dispatcher (flush window, hooks first, async status, retries), the **path planner with budget**, the actions, the widget and Data Health. The core hook in QQQ (after RC.4). The contract and Testcontainers suites. | 9–11 d | `0.1.0` is published, and kof22 runs HEADERS_ONLY with the would-be purges recorded. |
| **P2: CloudFront adapter + AWS/network IaC + media** | The CloudFront adapter (invalidation, status, health and drift, CloudWatch stats). Praetor `terraform/cdn-cloudfront` (ACM, distribution, policies, function, Key D, prefix-list output). octoDNS validation record, CAA, and the `origin`/`upload` records. The UniFi port 8443 and firewall group. The Traefik `cdn` entry point, `origin-guard` and the edge-header stripping. The owner subscribes to flat-rate Pro. **Cut over `s3`.** | 6–8 d, plus a soak period | `s3` is LIVE, the edge matrix is green, and a direct `:8443` request without the secret gets 403. |
| **P3: `www` Stage A + `api` + real IP** | Cut over `www` (with the apex) and `api`. ESB invalidation goes live. Analytics, `ClientIp` and `forward.ts` use `X-Real-IP`. The rate limiters move to `ipStrategy`. | 3–4 d | A publish is visible through the API in under ~2 min (flush window plus invalidation), and analytics shows real geo. |
| **P4: `www` Stage B (HTML)** | Remove `force-dynamic`. ISR with `cacheTag`. `/api/revalidate`. The pod fan-out hook. The HTML leaf and hub policies. Verify the RSC cache key. The warm action. | 5–7 d | A published post is correct at the edge in under ~2 min, HTML leaves are mostly HITs, and no personalized page is cached. |
| **P5: stats and polish** (optional) | Additional metrics and/or logs-based per-host stats. The budget projection. The CloudWatch alarm. The `GetDistributionConfig` drift view. | 2–3 d | The widget shows the hit rate (if enabled). |
| **P6: second adapter** (optional) | A Bunny or Cloudflare adapter with fixtures, run through the same e2e suite. | 3–4 d | Portability is proven. |
| **LOCK** | Drop `websecure` for each hostname after 2 weeks stable. | 0.5 d per hostname | Direct origin requests for those hosts get 404. |

**Total:** about **5.5–7.5 weeks** for P0–P4, plus 1–1.5 weeks for P5 and P6. **P0 can start now.**

---

## 12. Decisions

### 12.1 Decided (2026-10-12)

See D1–D15 in the table at the top of this document.

### 12.2 Remaining owner items (answered 2026-10-12)

| # | Question | Answer |
|---|---|---|
| O1 | Which AWS account? | **The existing account `744245396565`** (profile `kingsrook_root_admin`), with `tenant=kof22` cost-allocation tags and the `allowed_account_ids` guard. |
| O2 | Subscribe to the CloudFront flat-rate Pro plan ($15/mo) | **The owner subscribes himself once the distribution exists in P2.** The implementer stops at that moment and asks the coordinator. Whether one plan covers one distribution with 4 aliases still has to be checked then. |
| O3 | ACM certificate in `us-east-1` | **Approved:** `kof22.com` + `*.kof22.com`, DNS-validated through octoDNS (the validation CNAME goes in the zone file), and CAA extended with `amazon.com`. |
| O4 | WAN port 8443 | **Approved,** open only to CloudFront origin-facing ranges, through UniFi IaC. |
| O5 | New hostnames | **Approved:** DNS-only `origin.kof22.com` and `upload.kof22.com`. `upload` ships in P0. `origin` ships in P2. |
| O6 | Origin secret | **Infisical,** rotated every 90 days with two values valid during the overlap. |
| O7 | Metrics and logs | **Cache-hit-rate (additional) metrics only. No standard logs.** |
| O8 | Privacy page | **The privacy page names AWS as a processor.** |

---|---|---|
| O1 | **Which AWS account?** | **(a)** The existing account `744245396565` (profile `kingsrook_root_admin`), which already holds the Route53 zone. Add `tenant=kof22` cost tags and keep the `allowed_account_ids` guard. **Recommended.**<br>(b) A new AWS Organizations member account for kof22 with its own profile. Billing is cleaner, and cross-account Route53 ALIAS still works, but the account has to be set up. |
| O2 | **Subscribe to the CloudFront flat-rate Pro plan** ($15/mo). This is a billing action only the owner can take, after the distribution exists in P2. Also confirm the plan covers **one distribution with 4 aliases** (*verify* whether it attaches per distribution). | **(a)** Yes: one Pro plan on the single kof22 distribution. **Recommended.**<br>(b) Pay-as-you-go: about $0 at this volume, but no cost ceiling and no bundled WAF. |
| O3 | **ACM certificate in `us-east-1`:** which names, and how to validate | **(a)** `kof22.com` + `*.kof22.com`, validated by DNS. Add the validation CNAME to the octoDNS zone file and extend CAA with `amazon.com`. **Recommended.**<br>(b) Explicit names only: `kof22.com`, `www`, `api`, `s3`. |
| O4 | **Open WAN port 8443**, limited to CloudFront's origin-facing ranges (UniFi, as IaC) | **(a)** Approve. **Recommended.**<br>(b) Reuse 443 instead. No source restriction is possible there because other tenants share it, so protection rests on the secret header alone. Weaker. |
| O5 | **New hostnames** `origin.kof22.com` and `upload.kof22.com` (DNS-only) | **(a)** Approve both. **Recommended.**<br>(b) Different names. |
| O6 | **Origin secret:** where it is stored, and how it rotates | **(a)** An Infisical path for the kof22 website, rotated every 90 days with two valid values during the overlap. **Recommended.**<br>(b) A static secret, rotated by hand. |
| O7 | **Optional metrics and logs** for cache hit rate and per-host stats | **(a)** CloudFront additional metrics only (cache hit rate per distribution), with standard logs off. **Recommended.**<br>(b) Also standard logs v2, with 7-day retention, for per-host stats. These logs contain IPs (§7.4).<br>(c) Neither: requests, bytes and error rates only. |
| O8 | **Which AWS agreement and DPA cover kof22 visitor data** (privacy page wording) | Needs owner or legal input. Depends on O1. |

---

## 13. Risks

| # | Risk | Mitigation |
|---|---|---|
| R1 | A personalized or authenticated response is cached | `CachingDisabled` behaviors, the header-policy safety rule, the matrix tests, and owner sign-off on P4 |
| R2 | Stale content: invalidation takes minutes, a hook fails, or one Next pod is behind | Pods revalidate before the edge. Hub TTLs are short. DEAD rows alert. Manual purge is available. The publish SLA is stated as about 2 min, not instant. |
| R3 | The invalidation budget runs out | Hubs expire by TTL, wildcards collapse paths, the planner has budget bands, paid paths are capped, and the widget shows a projection. The worst case is a few dollars, or a DEFER until the TTL expires. |
| R4 | CloudFront's origin-facing ranges change and UniFi blocks the new ones | A weekly `syncToken` CI check, a PR to regenerate, and the `OriginGuardRejectSpike` and 5xx alarms. The 443 path is untouched, so a DNS rollback restores service. |
| R5 | The origin secret leaks | An attacker would still need a CloudFront source IP (UniFi filter), and with it a distribution of their own, plus the secret. Rotate through Infisical with the overlap. |
| R6 | Spoofed real-IP headers | They are trusted only when the guard validates the secret. They are stripped on `websecure`, and `authResponseHeaders` overwrites them. |
| R7 | Wrong `Vary` or cache key for Next RSC or image formats | Explicit cache-key headers (`RSC`, normalized `Accept`), matrix tests for both variants, and a re-check against the Next CDN guide on every Next upgrade |
| R8 | The flat-rate plan has feature limits (Lambda@Edge, logging, invalidation pricing) that differ from pay-as-you-go | *Verify* before P2 (O2). The design only needs CloudFront Functions, invalidations and CloudWatch metrics. |
| R9 | `origin-guard` is a single point of failure | 2 replicas, a PDB and an alert. The guard is stateless. Rollback is a DNS revert. |
| R10 | Release interference | No QQQ core change lands before RC.4 is cut. |
| R11 | octoDNS deletes the ACM validation record or the ALIAS records | Keep them in the zone YAML. Rely on `delete_pcent_threshold`, and review the octoDNS plan in the PR. |

---

## 14. Sources

Researched 2026-10-12. Prices change often.

- CloudFront flat-rate plans: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/flat-rate-pricing-plan.html , https://docs.aws.amazon.com/PricingPlanManager/latest/UserGuide/plans.html , https://aws.amazon.com/blogs/networking-and-content-delivery/introducing-flat-rate-pricing-plans-with-no-overages/
- CloudFront pricing: https://aws.amazon.com/cloudfront/pricing/ ; invalidation pricing: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/PayingForInvalidation.html ; invalidation how-to and quotas: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/Invalidation.html , https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/cloudfront-limits.html
- CloudFront private content / key groups: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/PrivateContent.html ; edge functions: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/edge-functions-choosing.html ; origin shield: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/origin-shield.html
- CloudFront origin-facing ranges and prefix list: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/LocationsOfEdgeServers.html , https://ip-ranges.amazonaws.com/ip-ranges.json ; custom origin headers: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/add-origin-custom-headers.html ; viewer headers (`CloudFront-Viewer-Address`): https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/adding-cloudfront-headers.html ; cache key and policies: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/controlling-the-cache-key.html ; real-time logs: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/real-time-logs.html
- Cloudflare purge: https://developers.cloudflare.com/cache/how-to/purge-cache/ ; tags: https://developers.cloudflare.com/cache/how-to/purge-cache/purge-by-tags/ ; `CDN-Cache-Control`: https://blog.cloudflare.com/cdn-cache-control/ ; images: https://developers.cloudflare.com/images/pricing/ ; R2: https://developers.cloudflare.com/r2/pricing/ ; Authenticated Origin Pulls: https://developers.cloudflare.com/ssl/origin-configuration/authenticated-origin-pull/
- Fastly Surrogate-Key: https://www.fastly.com/documentation/reference/http/http-headers/Surrogate-Key/ ; purging: https://www.fastly.com/documentation/guides/full-site-delivery/purging/getting-started-with-purging/ ; pricing: https://www.fastly.com/pricing
- Bunny purge and tags: https://bunny.net/docs/cdn/purge-cache , https://bunny.net/blog/introducing-tag-based-cdn-cache-purging/ ; token auth: https://bunny.net/docs/cdn/security/token-authentication/advanced ; pricing: https://bunny.net/pricing/cdn/
- Akamai cache tags: https://techdocs.akamai.com/purge-cache/docs/purge-content-cache-tag ; Azure Front Door: https://learn.microsoft.com/en-us/azure/frontdoor/front-door-cdn-comparison , https://learn.microsoft.com/en-us/azure/frontdoor/cache-purge ; Google Cloud CDN: https://docs.cloud.google.com/cdn/docs/cache-invalidation-overview
- Varnish xkey: https://docs.varnish-software.com/book/invalidation/secondary-keys/ ; Souin: https://github.com/darkweak/souin ; imgproxy: https://docs.imgproxy.net/usage/signing_url
- RFC 9111: https://www.rfc-editor.org/rfc/rfc9111 ; RFC 9110: https://www.rfc-editor.org/rfc/rfc9110 ; RFC 9213: https://httpwg.org/specs/rfc9213.html ; RFC 5861: https://www.rfc-editor.org/rfc/rfc5861 ; cache groups draft: https://datatracker.ietf.org/doc/draft-ietf-httpbis-cache-groups/
- Next.js self-hosting: https://nextjs.org/docs/app/guides/self-hosting ; CDN caching: https://nextjs.org/docs/app/guides/cdn-caching ; `expireTime`: https://nextjs.org/docs/app/api-reference/config/next-config-js/expireTime ; images: https://nextjs.org/docs/app/api-reference/config/next-config-js/images
- Traefik entry points and forwarded headers: https://doc.traefik.io/traefik/routing/entrypoints/#forwarded-headers ; forwardAuth: https://doc.traefik.io/traefik/middlewares/http/forwardauth/
- octoDNS Route53 provider (ALIAS): https://github.com/octodns/octodns-route53
- Internal: Praetor `terraform/dns-route53/{providers,iam}.tf`, `dns/route53/zones/kof22.com.yaml`, `docs/STATUS-2026-10-11-requests.md`; Website-Backend `docs/site-api.md`, `docs/cms-media.md`, `metadata/esb/WebsiteEsbMetaData`, `siteapi/http/SiteApiRouteProvider`; Website-Frontend `next.config.mjs`, `src/app/layout.tsx`, `src/lib/analytics/forward.ts`; cluster-gitops `tenants/kof22/website/*`, `platform/traefik/values.yaml`; QQQ `docs/specs/2026-09-25-qqq-esb-design.md`.
