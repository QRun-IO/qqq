# QQQ CDN qbit: Design Spec

Status: **Draft for owner review** · drafted 2026-10-12 · Author: James Maes (drafted with Claude)
Targets: QQQ 4.1.x (one small response-header hook in `qqq-middleware-javalin`), new repo `QRun-IO/qbit-cdn`, first consumer kof22 (`Website-Backend`, `Website-Frontend`, `cluster-gitops` tenants/kof22, Praetor `dns/route53`)

> Research and design only. Nothing in this document has been implemented. No accounts, keys, DNS records or cluster resources were created or changed. Every owner choice is collected in §12 as a multiple-choice decision with a recommendation first.

## 0. Summary

`qbit-cdn` lets any QQQ application put a CDN in front of its public site, its public API and its media without becoming tied to one CDN vendor. It does four things:

1. **Emits portable cache headers.** It writes standard `Cache-Control` for browsers and `CDN-Cache-Control` (RFC 9213) for shared caches. It also adds one cache-tag header per provider: `Cache-Tag`, `Surrogate-Key`, `CDN-Tag`, `Edge-Cache-Tag` or `xkey`. The values come from **cache policies** stored as normal QQQ records.
2. **Purges on change.** When a CMS record is inserted, updated or deleted, the ESB event becomes **cache tags** (for example `post:123`, `blog:7`, `media:456`) and **URLs**. The qbit merges events that arrive close together into as few provider purge calls as possible. It retries failures and keeps every request in a visible `cdnPurgeRequest` table.
3. **Builds URLs.** It rewrites media URLs to the CDN hostname. It builds image-transform URLs for `FILE_PREVIEW` and for the Next.js `next/image` loader. It signs URLs for private media when a provider supports that.
4. **Reports real data.** It imports edge statistics (requests, cache hit ratio, bytes) into `cdnEdgeStatDaily`. It feeds a CDN widget in Admin → Health that shows only imported or recorded data.

Providers plug in through one SPI, `CdnProviderInterface`. Each provider declares what it can do (its capabilities), and the qbit fills gaps in a fixed order. If a provider has no tag purge, the qbit expands each tag to URL templates, then to prefixes, then to hostname purges. A **`NONE`** provider makes the qbit safe to install before any CDN exists, because headers are still correct and purges are recorded as no-ops. A **self-hosted Souin/Varnish** adapter is the real target for automated end-to-end tests.

**What kof22 does today** (research, §1.2):
- No CDN: `kof22.com`, `www`, `api`, `admin` and `s3` all resolve to the home WAN IP `50.122.5.146`.
- The Next.js site is fully dynamic (`force-dynamic` in the root layout).
- Media objects are uploaded with **no** `Cache-Control`, even though their keys are already immutable and content-addressed.
- No one consumes the `kof22.cms.tables` ESB topic.
- Traefik does not see real client IPs, and that fix is paused waiting on the owner.

So the first gains are cheap: about **15 to 20 hours** of origin cleanup (Phase 0, §11) before any CDN is involved.

**Recommendation in one paragraph:** Start with **Cloudflare Free** for kof22, reached over a **Cloudflare Tunnel** into a dedicated Traefik entry point that is not port-forwarded. Roll out one hostname at a time: `s3`, then `www` static assets and images, then `api`, then `www` HTML. Never put `admin` or `auth` behind the CDN. This setup:
- costs $0/month
- has purge by tag, prefix and hostname on every plan
- hides and protects the origin without opening another port
- gives a trustworthy `CF-Connecting-IP`, which fixes the real-client-IP problem for every hostname behind the CDN.

**CloudFront flat-rate Pro ($15/month)** is the fallback if the owner wants to keep DNS entirely in Route53. Build the qbit provider-neutral, and add a second adapter (CloudFront or Bunny) only when it is needed. That second adapter is what proves the abstraction works.

---

## 1. Context

### 1.1 Goals

- Faster first byte and asset load for visitors far from the homelab, and less traffic on the residential uplink.
- One qbit that works with Cloudflare, CloudFront, Fastly, Bunny, Akamai, Azure Front Door, Google Cloud CDN, or a self-hosted cache, with no app code changes when switching.
- Correct by default: nothing personalized or authenticated is ever cached at the edge, and published changes appear within seconds.
- Protect the origin. Once a hostname is cut over, only the CDN can reach it.
- Follow the owner's qbit rules:
  - qbit data lives in standard QQQ tables, secured by QQQ permissions and locks
  - processes run as table or record actions, never from the nav
  - only real data is shown
  - the UI is readable at every width.

### 1.2 Current state (kof22, researched 2026-10-12)

| Area | Today | CDN relevance |
|---|---|---|
| DNS | Route53 zone `kof22.com`, managed by octoDNS in Praetor `dns/route53/zones/kof22.com.yaml`. The zone and DNSSEC are in tofu `terraform/dns-route53`. `kof22.com`, `www`, `admin`, `api` (TTL 60) and the DSM suite hosts are A records to `50.122.5.146`. `s3`, `auth` and `minio-console` are CNAMEs to `www`. CAA allows only `letsencrypt.org`. | The CDN choice decides where DNS lives (D2). The CAA record must allow the CDN's CA. |
| Edge path | UniFi port-forward → Cilium LB `10.100.22.64` → Traefik, a single replica, with `externalTrafficPolicy` left at the default `Cluster` (SNAT). `forwardedHeaders.trustedIPs` is set to the RFC1918 ranges. | Traefik sees SNAT addresses, not visitors. Behind a CDN, the viewer header must be trusted **only** on a path that only the CDN can reach (§7.3). |
| TLS | cert-manager Let's Encrypt via Route53 DNS-01. The Traefik default cert is the wildcard `kof22-wildcard-tls`. | Keep it as the origin cert. The CDN issues the edge cert. |
| Media | MinIO public bucket `kof22-website-files` at `https://s3.kof22.com/kof22-website-files/...`. Keys are `media/{siteId}/{slug}/{sha256}/original.{ext}` plus `thumbnail.jpg` (400px), `gallery.jpg` (1600px) and `slideshow.jpg` (2400px), and `photos/{album}/{photo}/...`. Renditions are made at upload with Thumbnailator. **Uploads set no `Cache-Control`.** The private `kof22-website-incoming` bucket gets presigned PUTs that expire in 1 day. | Ideal for a CDN: keys are immutable, so objects can be `max-age=31536000, immutable`. |
| Site API | `api.kof22.com` (Traefik `addPrefix /site-api`). `SiteApiRouteProvider.finish()` sets `public, max-age=60` on anonymous 200 GETs: 300 for docs, sitemap and feeds, and `no-store` for `/v1/me*` and contact. It strips `Set-Cookie` and adds `Vary: Origin`. Reads have no ETag and no 304. | It is almost CDN-ready. Add `CDN-Cache-Control`, tags and conditional GETs, and reconsider `Vary: Origin` (§5.4). |
| Frontend | Next.js `output: 'standalone'`, 2 replicas, `force-dynamic` in the root layout. There is no ISR, `revalidate`, tag, custom `cacheHandler`, `assetPrefix` or revalidation route. `next/image` uses the built-in `/_next/image` optimizer, whose cache is per-pod disk. | Static assets and optimized images can be cached at the edge right away. HTML caching first needs ISR (§6). |
| ESB | Artemis. Topic `kof22.cms.tables` gets INSERT/UPDATE/DELETE for 16 CMS tables. Topic `kof22.cms.processes` gets `PublishBlogPost` lifecycle events. **No one consumes `kof22.cms.tables`.** | The purge pipeline subscribes here (§5.3). |
| Analytics | Privacy-first: a beacon calls `/api/e`, which forwards to the backend `/analytics/v1/collect`. The visitor hash is SHA-256(daily salt, site, IP, UA). The IP is never stored. GeoLite2 is city-level. A SNAT IP produces `GeoStatus.PROXY_IP`. | A CDN header gives the real IP (§7.4). The CDN must not inject its own analytics (D13). |
| QQQ core | Has no Cache-Control or ETag handling in `QJavalinImplementation` or the API middleware. Only `NextDashboardRouteProvider` (`_next/static/` gets `immutable`) and `BrandingAssetsRouteProvider` set cache headers. | The qbit needs one small core hook (§4.6). |

### 1.3 Non-goals (v1)

- Caching anything on `admin.*`, `auth.*`, the QQQ Next dashboard or `/v1/me*`. Authenticated surfaces stay on the origin path.
- Running edge compute (Workers, Lambda@Edge, Compute) as part of the qbit. Edge code, if ever needed, belongs to infrastructure.
- Pushing CDN configuration (cache rules, WAF, TLS) from the app. Infrastructure as code owns that, and the qbit only reads and reports drift (D9).
- Video streaming. Large video belongs to a purpose-built product such as Cloudflare Stream, R2 or MediaConvert, not to a general CDN plan (risk R6).

---

## 2. Research summary

Sources are listed in §14. Prices are as of 2026-10-12 and change often. Items marked *verify* were not confirmed against a primary source in this session.

### 2.1 Provider comparison

| | Cloudflare | AWS CloudFront | Fastly | Bunny.net | Google Cloud CDN | Azure Front Door | Akamai | Self-hosted (Souin / Varnish) |
|---|---|---|---|---|---|---|---|---|
| **Purge by URL** | Yes | Yes (invalidation paths) | Yes | Yes | Yes (path) | Yes (path) | Yes | Yes |
| **Purge by prefix / wildcard** | Yes, **all plans** | Yes (`/path*` counts as 1 path) | Via key | Yes (wildcard URL) | Yes (prefix, host) | Yes (wildcard) | Via CP code | Varnish BAN |
| **Purge by tag** | Yes, **all plans** (`Cache-Tag`) | **No** | Yes (`Surrogate-Key`, batch API, soft purge) | Yes (`CDN-Tag`, wildcard) | Yes (`Cache-Tag`, 50 tags per object, 10 tags per request) | **No** | Yes (`Edge-Cache-Tag`, Fast Purge v3) | Yes (Souin `Surrogate-Key`; Varnish `xkey`) |
| **Purge limits** | Tag/prefix/host: Free 5 req/**min** (bucket 25), Pro 5 req/s. Up to 100 items per request. | First 1,000 paths/month free, then about $0.005 per path | About 100k purges/hour | Not published (*verify*) | 500 requests/min, about 10 s to take effect | About 100 concurrent (*weak source*) | Plan-dependent | None |
| **Signed URLs** | No native signed URLs on self-serve plans. Use WAF HMAC validation (plan-gated, *verify*) or a Worker. | Yes: key groups, canned or custom policy, URLs or cookies | Via VCL or Compute | Yes: token auth, HMAC-SHA256 "advanced" | Yes: URLs and cookies | Yes (Premium: token auth, *verify*) | Yes (EdgeAuth tokens) | imgproxy HMAC (images only) |
| **Image transforms** | `/cdn-cgi/image/<opts>/<src>`. 5,000 unique transforms/month free, then $0.50 per 1,000 | None native. Run your own (Lambda or imgproxy). | Image Optimizer: 100k free, then $0.50 per 10k | Bunny Optimizer $9.50 per site per month | None native | None native | Image & Video Manager | imgproxy or thumbor (self-hosted) |
| **Origin protection** | **Tunnel** (outbound only, no inbound port), Authenticated Origin Pulls (global, zone or per-hostname mTLS), published IP ranges | Secret custom origin header plus allowlist from `ip-ranges.json` (managed prefix lists only work inside AWS) | Published IPs, shared-secret header, mTLS | Published edge IPs, secret header (*verify*) | Internet NEG plus ALB, published IPs | Private Link (Premium, Azure origins), header plus IPs | Site Shield, mTLS | Not needed (in-cluster) |
| **Real client IP header** | `CF-Connecting-IP` (`True-Client-IP` on Enterprise) | `CloudFront-Viewer-Address` (needs an origin request policy) | `Fastly-Client-IP` | `CDN-ClientIP` (*verify*) | `X-Forwarded-For` (the client is the first entry) | `X-Forwarded-For`, `X-Azure-ClientIP` (*verify*) | `True-Client-IP` | Original `X-Forwarded-For` |
| **Analytics API** | GraphQL Analytics API (all plans). Logpush of HTTP requests is Enterprise only (*verify*). | CloudWatch metrics, standard logs v2, real-time logs (Kinesis) | Real-time stats API, log streaming | Statistics API | Cloud Logging | Metrics and logs in Azure Monitor | Reporting API, DataStream | Prometheus exporters |
| **Honors `CDN-Cache-Control` (RFC 9213)** | Yes (co-author) | No (use the `s-maxage` policy) | Co-author; default behavior *verify* (it honors `Surrogate-Control`) | No (use `s-maxage` or edge rules, *verify*) | No (`s-maxage`) | No | `Akamai-Cache-Control` / targeted (*verify*) | Souin: yes |
| **IaC** | OpenTofu `cloudflare` provider (`cloudflare_ruleset`, phase `http_request_cache_settings`). octoDNS has a Cloudflare provider. | OpenTofu `aws` provider. Route53 alias works for the apex. | `fastly` provider | `bunnynet` provider (*verify*) | `google` provider | `azurerm` provider | `akamai` provider | Helm or GitOps |
| **Cost for kof22-scale traffic** (100–500 GB, ~3M req/mo) | **$0** (Free). Pro ~$20–25 (*verify*). | **$0** pay-as-you-go (always-free 1 TB and 10M requests). Flat-rate **Pro $15** (Free plan caps at 1M requests / 100 GB). | Free tier is 100 GB / 100k requests, which kof22 exceeds. Packages start at $1,500/mo. | **$1–5** ($0.01/GB in NA/EU, $1 minimum) | ~$10–40 plus about $18 for the ALB (*verify*) | ~$35 base plus usage (*verify*) | Enterprise contract | $0, but **all traffic still goes over the home uplink** |
| **Notes** | Proxying needs the zone on Cloudflare DNS, a Business-plan CNAME setup, or Cloudflare for SaaS (D2). The self-serve plan terms favor web content over bulk video (R6). | Keeps everything in AWS and Route53. ACM viewer certs need CAA for Amazon. No tag purge, so the qbit expands tags to URL templates. | Best-in-class tag purge, too expensive here | Cheapest paid option, simple API, good tag purge | Overkill for a homelab origin | Classic Front Door retires 2027-03-31. Classic Microsoft CDN retires 2027-09-30. | Enterprise only | Only useful as a dev/test adapter or an internal cache |

**Takeaways for the design:**

1. **Cache tags are common but not universal.** CloudFront and Azure have no tag purge, so the qbit must be able to turn a tag into URL patterns (§4.3).
2. **Tag header names and limits differ.** The strictest common limits are Bunny's 1 KB `CDN-Tag` header and Google's 50 tags per object and 120 bytes per tag. The qbit's **portable tag budget** is therefore ≤ 20 tags per response, each ≤ 64 ASCII characters, and ≤ 1,000 bytes in total. If a response would go over, the qbit collapses fine-grained tags into a coarse tag (§4.2).
3. **`CDN-Cache-Control` is honored by Cloudflare and Souin.** Other providers need `s-maxage` or provider rules, so the qbit always emits **both**: `Cache-Control` with `s-maxage` and `CDN-Cache-Control`. They have the same edge TTL, so either one is correct.
4. **Purge rate limits vary by orders of magnitude.** Cloudflare Free allows 5 tag-purge requests per **minute**. So the qbit must **merge** pending purges (one request carries up to 100 tags on Cloudflare) and **rate-limit per provider**.
5. **Next.js does not purge any CDN.** Its CDN guide says the app must call the purge API itself, for both the HTML and the RSC (`_rsc`) variants, after `revalidateTag` or `revalidatePath`. Its self-hosting guide also says that with more than one pod, revalidation must reach every pod or the tags must be shared (§6.3).

### 2.2 Standards the qbit relies on

| Standard | Use |
|---|---|
| RFC 9111 HTTP Caching | `max-age`, `s-maxage`, `public`/`private`/`no-store`, `immutable` (RFC 8246), how `Vary` works |
| RFC 9213 Targeted Cache-Control | `CDN-Cache-Control` for shared CDN caches, separate from browser `Cache-Control` |
| RFC 5861 | `stale-while-revalidate` and `stale-if-error`. The origin can be down for a while without visitors noticing. |
| RFC 9110 §8.8 / §13 | `ETag`, `Last-Modified`, `If-None-Match` → 304, which makes CDN revalidation cheap |
| RFC 9211 Cache-Status | Normalized HIT/MISS reporting (Souin emits it; other providers use their own headers, and the adapters normalize them) |
| W3C Edge Architecture (`Surrogate-Control`) | Older targeted header. Emitted only for Fastly, and only if D1 picks Fastly. |
| Tag headers | `Cache-Tag` (Cloudflare, Google), `Surrogate-Key` (Fastly, Souin), `CDN-Tag` (Bunny), `Edge-Cache-Tag` (Akamai), `xkey` (Varnish). The IETF draft `draft-ietf-httpbis-cache-groups` may standardize this later, so the qbit keeps the header name in the adapter. |

---

## 3. Architecture

```
                               ┌────────────────────────── qbit-cdn (in each QQQ app) ──────────────────────────┐
 CMS edit / publish            │                                                                                 │
 (QQQ actions) ──commit──► ESB │  CdnPurgeTrigger (EsbTrigger, BATCH)                                            │
 topic kof22.cms.tables ───────┼─► CdnTagResolver (cdnTableTagRule) ─► cdnPurgeRequest rows (PENDING)           │
                               │                                           │                                     │
                               │  CdnPurgeDispatcher (scheduled, every 5 s, single runner)                       │
                               │     merge ─► rate-limit per provider ─► [revalidation hooks: Next pods] ─►      │
                               │     CdnProviderInterface.purge(tags|urls|prefixes|hosts)                        │
                               │                                                                                 │
 HTTP response (Site API,      │  CdnResponseHeaderPolicy (cdnCachePolicy) ─► Cache-Control, CDN-Cache-Control,  │
 QQQ API, route providers) ◄───┼─    tag header in the provider's dialect, ETag/304                              │
                               │                                                                                 │
 FILE_PREVIEW / Site API /     │  CdnUrlBuilder: media URL rewrite, image transform URL, signed URL               │
 Next loader ◄─────────────────┼─                                                                                │
                               │                                                                                 │
 Admin → Health widget ◄───────┼─ cdnEdgeStatDaily ◄── CdnStatsImport (scheduled, daily + on demand)            │
                               └─────────────────────────────────────────────────────────────────────────────────┘
                                        │ provider adapters: CLOUDFLARE · CLOUDFRONT · FASTLY · BUNNY · SOUIN · VARNISH · NONE
                                        ▼
                     Visitor ──► CDN edge ──(Tunnel or allowlisted origin pull)──► Traefik "cdn" entry point ──► www / api / s3 services
```

### 3.1 Where code lives (D10)

| Piece | Location | Why |
|---|---|---|
| SPI, adapters, tables, processes, widget, header policy, URL builder | New repo **`QRun-IO/qbit-cdn`** (package `com.kingsrook.qbits.cdn`), built with the `qbit-template` pattern (`CdnQBitConfig implements QBitConfig`, `CdnQBitProducer implements QBitMetaDataProducer<CdnQBitConfig>`) | Same layout as `qbit-quick-search`. Apps opt in. |
| **Response header hook** | QQQ core, `qqq-middleware-javalin`: a `QJavalinResponseHeaderCustomizerInterface` registered on `QJavalinMetaData`. It is called after the QQQ API and table GET handlers, with table, record and route context. | Today the API middleware gives no way to set headers per response. This is the only core change, about 1 day, and it is generic, so security headers and others can use it too. |
| Next.js helpers | Small npm package `@qrun-io/cdn-next`, or a folder copied into Website-Frontend in v1: an image loader, a header helper and the revalidate route | Keeps the frontend independent of the vendor |
| Edge configuration (DNS, cache rules, WAF, tunnel, TLS) | Praetor (OpenTofu plus octoDNS) and cluster-gitops (cloudflared, Traefik entry point) | Infrastructure as code is the source of truth (D9) |

### 3.2 Request classes and default policies

The qbit seeds these as `cdnCachePolicy` records (§4.4). Admins can tune them, and the e2e test matrix (§9.3) pins them.

| Class | Example | Browser `Cache-Control` | Edge (`s-maxage` / `CDN-Cache-Control`) | Tags | Notes |
|---|---|---|---|---|---|
| Immutable media | `s3…/media/1/x/<sha>/gallery.jpg` | `public, max-age=31536000, immutable` | 1 year | `media:<id>` | The key changes when the content changes. Purge is only needed on delete or takedown. |
| Hashed build assets | `/_next/static/*` | `public, max-age=31536000, immutable` (Next sets this) | 1 year | none | Never purged |
| Optimized images | `/_next/image?url=…&w=…&q=…` | `public, max-age=86400` | 1 year (the source is immutable) | `media:<id>` when it can be derived | Next `images.minimumCacheTTL` must be raised (§6.2) |
| Public API read | `api…/v1/blog-posts/123` | `public, max-age=60` | 1 day, `stale-while-revalidate=60`, `stale-if-error=86400` | `post:123`, `blog:7`, `site:1` | Purged on change, so a long edge TTL is safe |
| Public API list, feed, sitemap | `/v1/blog-posts?blog=…`, `/v1/feeds/*` | `public, max-age=60` | 1 hour, swr 60, sie 1 day | `blog:7:list`, `site:1:feeds` | Coarse tags |
| Public HTML (after ISR, §6) | `/blog/quick-bits/my-post` | `public, max-age=0, must-revalidate` | Next ISR `s-maxage`, swr | path tags (§6.4) | Browsers always revalidate, and the edge holds the page |
| Never cache | `/v1/me*`, `/v1/contact`, `/api/e`, `/preview`, `/api/*` (frontend), `admin.*`, `auth.*` | `private, no-store` | `no-store` | none | The edge also bypasses these by rule (defense in depth) |

---

## 4. The SPI and QQQ integration

### 4.1 `CdnProviderInterface`

```java
public interface CdnProviderInterface
{
   CdnProviderType getType();                                   // CLOUDFLARE, CLOUDFRONT, FASTLY, BUNNY, AKAMAI, AZURE_FRONT_DOOR, GOOGLE_CDN, SOUIN, VARNISH, NONE
   Set<CdnCapability> getCapabilities(CdnProviderConfig config); // PURGE_URL, PURGE_PREFIX, PURGE_TAG, PURGE_HOST, PURGE_ALL, SOFT_PURGE,
                                                                 // SIGNED_URL, SIGNED_COOKIE, IMAGE_TRANSFORM, STATS_IMPORT, RULES_READ, HONORS_CDN_CACHE_CONTROL
   CdnHeaderDialect getHeaderDialect();                         // tag header name, separator, max tags/bytes, whether the edge strips it,
                                                                 // cache-status header name + parser
   CdnPurgeLimits getPurgeLimits(CdnProviderConfig config);     // items per request, requests per window, per kind
   CdnPurgeResult purge(CdnPurgeInput input) throws CdnException; // one already-merged, already-sized batch
   CdnHealth checkHealth(CdnProviderConfig config);             // credentials valid, zone/distribution exists, token scopes
   default List<CdnEdgeStat> importStats(CdnStatsInput input)   { throw new CdnCapabilityException(STATS_IMPORT); }
   default String signUrl(CdnSignInput input)                   { throw new CdnCapabilityException(SIGNED_URL); }
   default CdnRulesSnapshot readRules(CdnProviderConfig config) { throw new CdnCapabilityException(RULES_READ); }
}
```

Image transforms have their own SPI, **`ImageTransformProviderInterface`** (`buildUrl(source, width, height, fit, quality, format)`, `getCapabilities()`). The image engine is often not the CDN: for example CloudFront plus imgproxy, or Cloudflare with the existing pre-made renditions. The implementations are:
- `RENDITIONS`: picks the nearest existing `thumbnail`/`gallery`/`slideshow` key, with zero compute
- `CLOUDFLARE_IMAGES` (`/cdn-cgi/image/...`)
- `IMGPROXY` (HMAC-signed, self-hosted)
- `BUNNY_OPTIMIZER`
- `NEXT_DEFAULT` (`/_next/image`)
- `NONE`

Adapters are stateless and take a `CdnProviderConfig` that holds identifiers and a **secret reference**, never the secret itself (§7.5). Each HTTP adapter uses the JDK `HttpClient` and has no vendor SDK dependency. The one exception is CloudFront, where AWS SDK v2 `cloudfront` is acceptable because SigV4 signing is not worth reimplementing.

### 4.2 Canonical cache tags

Tags are defined once, provider-neutral, as short ASCII strings: `<kind>:<id>[:<facet>]`. Examples:

| Tag | Emitted on | Purged when |
|---|---|---|
| `site:1` | every public response for site 1 | site settings, menus, snippets (coarse) |
| `post:123` | API read of post 123; HTML of that post | `blog_post` 123 or its current `blog_post_version` changes |
| `blog:7` | blog 7 detail, and every post in blog 7 | the blog itself changes |
| `blog:7:list` | lists, feeds and the sitemap section for blog 7 | any post in blog 7 is inserted, deleted, or changes publish state, title, slug, excerpt or featured image |
| `page:12` | page 12 and its sections | `page` or `page_section` change |
| `media:456` | media read, optimized-image responses | `media` 456 update or delete (renditions are immutable, so this is mainly for takedowns) |
| `author:3` | author reads and the posts that embed the author card | `author` change |
| `path:/blog/quick-bits/my-post` | HTML (§6.4) | the URL template expansion of the record |

**Budget enforcement** (`CdnTagBudget`): emit at most 20 tags, each ≤ 64 characters, with a header ≤ 1,000 bytes. When a list response would go over, the item tags are replaced by the list tag (`blog:7:list`), so there is one tag instead of 50 `post:*`. This rule is what keeps the tags portable to Bunny and Google. The adapter's `CdnHeaderDialect` then formats them: comma-separated `Cache-Tag` for Cloudflare, space-separated `Surrogate-Key` for Fastly and Souin, and so on. If the edge does not strip the tag header before it reaches visitors, the adapter declares `stripsTagHeader=false` and the edge rule removes it. This is listed in the IaC checklist (§8.4).

### 4.3 Purge planning and capability fallback

`CdnPurgePlanner` turns a set of tags into calls the provider can actually make:

1. If the provider has `PURGE_TAG`, send tags in batches of up to `limits.itemsPerRequest`.
2. Otherwise, use the `urlTemplates` of each `cdnTableTagRule` to expand every tag into URLs, such as `/blog/{blog.slug}/{slug}`, `/blog/{blog.slug}`, `/`, `/feed.xml` and `/sitemap.xml`. Add the `?_rsc=*` variants as a prefix or wildcard where the provider supports it. On CloudFront, `/blog/quick-bits/my-post*` counts as **one** path, and it covers the RSC variants.
3. If the URL count goes over a safety threshold (default 200), fall back to `PURGE_PREFIX`, and then `PURGE_HOST`.
4. `PURGE_ALL` is never chosen automatically. It is only available through the guarded "Purge everything" action (§4.7), which needs the `cdn.purgeAll` permission.

Purges are **idempotent and order-independent**, which matches ESB at-least-once, unordered delivery. A duplicate event only produces a duplicate purge, and merging usually absorbs it.

### 4.4 QQQ tables

Every table is a normal, visible QQQ table in the app's backend, prefixed by `CdnQBitConfig.tableNamePrefix` (default `cdn`). Each one has `id`, `createDate`, `modifyDate` and audit enabled. The `cdn.*` permissions are in §4.8.

| Table | Purpose | Key fields |
|---|---|---|
| **`cdnProvider`** | One row per configured CDN or image engine | `name`, `providerType` (PVS), `role` (CDN / IMAGE_TRANSFORM), `enabled`, `accountRef`, `zoneOrDistributionId`, `secretRef` (the name of an env var or Infisical key; **never the value**), `defaultForApp`, `lastHealthStatus`, `lastHealthCheckDate`, `capabilities` (read-only display computed from the adapter) |
| **`cdnHostname`** | Which public hostnames sit behind which provider, and their rollout stage | `hostname`, `cdnProviderId`, `hostnameRole` (SITE, API, MEDIA, ASSETS), `stage` (OFF / HEADERS_ONLY / LIVE), `purgeEnabled`, `originHost`, `notes` |
| **`cdnCachePolicy`** | Header policy per hostname, path or route class (§3.2) | `name`, `cdnHostnameId` (nullable means all), `matchType` (PREFIX, GLOB, ROUTE_CLASS, TABLE), `matchValue`, `tableName` (for TABLE matches), `browserMaxAge`, `edgeMaxAge`, `staleWhileRevalidate`, `staleIfError`, `immutable`, `noStore`, `varyHeaders`, `emitTags`, `priority`, `enabled`, `seeded` (true for defaults the qbit owns; editing one sets `seeded=false` so upgrades don't overwrite it) |
| **`cdnTableTagRule`** | How a table's changes become tags and URLs | `tableName`, `tagTemplates` (e.g. `post:{id}`, `blog:{blogId}:list`), `urlTemplates` (e.g. `/blog/{blog.slug}/{slug}`), `onlyWhenFieldsChange` (optional list, so autosaves of draft-only fields don't purge), `publishedFilter` (skip records that were never public), `enabled` |
| **`cdnPurgeRequest`** | Every purge, from every source | `cdnProviderId`, `kind` (TAG / URL / PREFIX / HOST / ALL), `targets` (text, one per line), `targetCount`, `source` (ESB_EVENT, RECORD_ACTION, MANUAL, DEPLOY, RETRY), `sourceTable`, `sourceRecordId`, `causationId` (from the ESB `qqqcausationid`), `requestedByUserId`, `status` (PENDING, MERGED, SENT, SUCCEEDED, FAILED, DEAD, SKIPPED_NONE_PROVIDER, SKIPPED_STAGE_OFF), `mergedIntoId`, `attempts`, `nextAttemptDate`, `providerRequestId`, `httpStatus`, `errorMessage`, `sentDate`, `completedDate` |
| **`cdnRevalidationHook`** | Downstream caches to refresh **before** the CDN purge, such as the Next.js pods | `name`, `url` (e.g. `http://website-frontend-headless.kof22-website-prod.svc:3000/api/revalidate`), `fanOutDns` (resolve every pod A record), `secretRef`, `timeoutMs`, `enabled` |
| **`cdnEdgeStatDaily`** | Imported edge stats, real data only | `cdnProviderId`, `hostname`, `statDate`, `requests`, `cachedRequests`, `hitRatio` (computed), `bytes`, `cachedBytes`, `status4xx`, `status5xx`, `threatsBlocked` (if provided), `importedDate`, `importRunId`. Unique on (provider, hostname, date). |

Optional and later: `cdnRulesSnapshot` (read-only rule dumps for the drift view, D9) and `cdnSignedUrlGrant` (audit of signed URLs that were issued, if private media arrives, D12).

### 4.5 ESB-driven purge

- The qbit registers an `EsbTrigger` on the app's CMS topic (kof22: `kof22.cms.tables`) that runs the process **`cdnEnqueuePurgeFromEvents`**. It runs in `BATCH` mode, with concurrency 1, `maxAttempts` 5 and the default DLQ.
  - For each CloudEvent (`qqq.table.<table>.inserted|updated|deleted`, with `data.record`/`data.oldRecord`), it finds the matching `cdnTableTagRule` and skips the event if `onlyWhenFieldsChange` or `publishedFilter` does not match.
  - It renders the tag templates from **both** `record` and `oldRecord`, so a changed slug or a moved blog purges the old location too. It then inserts one `cdnPurgeRequest` (kind TAG, status PENDING) per provider whose hostnames are `LIVE`.
  - If no hostname is `LIVE`, the row is recorded as `SKIPPED_STAGE_OFF`. That shows what *would* have been purged, which is useful in rollout stage HEADERS_ONLY.
- **`cdnDispatchPurges`** is a scheduled process (every 5 s, single runner via the QQQ scheduler's clustered lock). It works per provider:
  1. Merge PENDING rows into as few calls as `getPurgeLimits` allows, and mark the absorbed rows `MERGED` with `mergedIntoId`.
  2. Wait if the provider's token bucket is empty. Cloudflare Free allows 5 tag purges per minute, so merging normally keeps kof22 well under that.
  3. **Call each enabled `cdnRevalidationHook` first.** For Next.js, every pod revalidates the same tags (§6.3). This step must finish before the CDN purge. Otherwise the edge refetches a stale page from Next and caches it again.
  4. Call `provider.purge`, and record the outcome, the provider request id and the HTTP status.
  5. On failure, back off exponentially (5 s, 30 s, 2 min, 10 min, 1 h). After 5 attempts, set the status to `DEAD`. Content still corrects itself within the edge TTL plus `stale-while-revalidate`. The worst case is ≤ 1 day for API items, and that is why HTML edge TTLs stay short (§6).
- **Publish without ESB.** Apps without the ESB can call `CdnPurgeService.enqueue(tags, source)` directly from a post-commit callback. The ESB route is preferred, because it survives restarts and has a DLQ.

### 4.6 Response headers

- **`CdnResponseHeaderPolicy`** finds the highest-priority enabled `cdnCachePolicy` for the request (hostname, path or route class, table) and writes:
  - `Cache-Control: public, max-age=<browser>, s-maxage=<edge>[, stale-while-revalidate=…][, stale-if-error=…][, immutable]`. Or `private, no-store`.
  - `CDN-Cache-Control: max-age=<edge>[, stale-while-revalidate=…][, stale-if-error=…]`, emitted whenever there is an edge TTL. It is harmless where the provider ignores it.
  - The tag header in the provider's dialect, built from the record context (table and primary key, plus `cdnTableTagRule` templates) and limited by the budget.
  - A weak `ETag` (`W/"<table>:<id>:<modifyDate epoch ms>"` for single records, or a hash of ids and modifyDates for lists). It answers `If-None-Match` with **304**, so revalidation between the CDN and the origin costs almost nothing.
  - **Safety rule:** if the response has `Set-Cookie`, an `Authorization`-bearing request, or a non-anonymous session, the policy forces `private, no-store` and emits no tags, whatever the policy says.
- **Wiring:**
  - QQQ API and table GETs use the new core `QJavalinResponseHeaderCustomizerInterface` (§3.1).
  - kof22's custom `SiteApiRouteProvider.finish()` calls `CdnResponseHeaderPolicy.apply(ctx, routeClass, recordContext)` instead of its hard-coded `max-age` values. The current values become the seeded policies, so nothing changes until a policy is edited.
- **MinIO objects** cannot carry dynamic headers, so they get two things:
  - (a) `Cache-Control: public, max-age=31536000, immutable` set in `ObjectMetadata` at upload (`PhotoImageProcessor.uploadToS3`, `MediaImageProcessor`)
  - (b) a one-time backfill (copy-in-place with `REPLACE` metadata) for existing keys
  - (c) belt and braces: an edge cache rule for `s3.kof22.com/kof22-website-files/*` with edge TTL 1 year.

  Tags for media come from the key path (`media/{siteId}/{slug}/{sha}`). A Traefik `headers` middleware on the `cdn` entry point can add `Cache-Tag: media-key:<sha>` only if takedown by tag is needed. In v1, media takedown uses URL purge of the four rendition URLs, which the `media` record already lists.

### 4.7 Processes (table and record actions only, never nav)

| Process | Where it appears | Permission | What it does |
|---|---|---|---|
| `cdnPurgeRecordUrls` | Record action **"Purge from CDN"** on every table with an enabled `cdnTableTagRule` (blog_post, page, media, ...), and as a bulk action on selected rows | `cdn.purge` | Review screen listing the tags, the expanded URLs and the target providers, then enqueue with `source=RECORD_ACTION`, then a result screen with links to the `cdnPurgeRequest` rows |
| `cdnWarmRecordUrls` | Record action **"Warm CDN cache"** (same tables) | `cdn.purge` | Requests the expanded URLs through the public hostname (HTML plus the `_rsc` variant) with a polite concurrency of 2, and reports the cache status of each one. Real responses only. |
| `cdnRetryPurge` | Record action on `cdnPurgeRequest` (FAILED or DEAD) | `cdn.purge` | Requeues it with `source=RETRY` |
| `cdnPurgeHostname` | Record action on `cdnHostname` | `cdn.purgeAll` | Guarded confirmation that requires typing the hostname. Purges by host if the provider can, otherwise purges everything for that zone. |
| `cdnTestProvider` | Record action on `cdnProvider` | `cdn.admin` | `checkHealth()`: credentials, token scopes, zone or distribution, and a no-op purge of a sentinel tag |
| `cdnImportEdgeStats` | Table action on `cdnEdgeStatDaily`, also scheduled daily at 02:15 | `cdn.admin` | Imports the last N days for each LIVE hostname. Upserts. |
| `cdnBackfillObjectHeaders` | Table action on `cdnHostname` (MEDIA role) | `cdn.admin` | One-time MinIO metadata backfill (§4.6). Dry-run first, showing counts. |

### 4.8 Permissions and locks

- `cdn.view` lets a user read every `cdn*` table and the widget.
- `cdn.purge` lets a user run purge, warm and retry actions.
- `cdn.purgeAll` lets a user purge a whole hostname.
- `cdn.admin` lets a user edit providers, hostnames, policies, tag rules and hooks, and run tests and imports.
- `cdnPurgeRequest` rows are never edited by hand. The table is insert-only from processes, and its fields are read-only in the UI.
- Secrets never appear in any table (`secretRef` only).
- For multi-tenant apps, each table carries the app's tenant key with a `RecordSecurityLock`, the same way `qbit-assistant` does (§D6 of that spec).

### 4.9 Admin → Health: CDN widget

A `CdnHealthWidget`, contributed to the Admin → Health area the way other qbits contribute widgets. Everything in it comes from table data. When there is no data, it says so ("No edge statistics imported yet. Run *Import edge stats* or wait for the 02:15 schedule.") and never draws placeholder numbers.

- **Per LIVE hostname:** the 7-day cache hit ratio and requests and bytes served from cache, from `cdnEdgeStatDaily`, with a sparkline of daily hit ratio.
- **Purge pipeline:**
  - PENDING count, and the age of the oldest PENDING row
  - SUCCEEDED, FAILED and DEAD counts for the last 24 h
  - median submit-to-complete latency
  - links to filtered `cdnPurgeRequest` lists.
- **Provider health:** last `checkHealth` status and time.
- **Layout:** stacks to a single column below 640 px. Tables become label/value rows, per the readable-at-all-widths rule.

Data Health rules (where the app uses QQQ Data Health): `operations.cdn-dead-purges` (any DEAD in the last 24 h), `operations.cdn-purge-backlog` (oldest PENDING older than 5 min) and `operations.cdn-stats-stale` (no import in 48 h).

### 4.10 URL building

- **`CdnUrlBuilder.mediaUrl(key)`** returns `https://<media hostname>/<bucket>/<key>`. When the MEDIA hostname is `LIVE`, the CDN serves it, and the URL stays the same, because the CDN fronts `s3.kof22.com` itself. This is a deliberate choice: **no stored URLs change**. The `publicUrl`, `thumbnailUrl`, `featuredImage` and similar values stay valid, and rollback is pure DNS. A separate media hostname, such as `media.kof22.com`, is optional later. If it is adopted, `CdnUrlBuilder` becomes the single place that formats URLs, and a migration rewrites the stored ones.
- **`CdnUrlBuilder.imageUrl(source, w, h, fit, q)`** delegates to the configured `ImageTransformProviderInterface`. `FILE_PREVIEW` thumbnails in the admin keep using `thumbnailUrl` (the 400 px rendition). The builder serves the Site API's optional `srcset` hints and the Next loader (§6.2).
- **`CdnUrlBuilder.signedUrl(key, ttl)`** uses the provider's `signUrl` when it has `SIGNED_URL`. Otherwise it falls back to a MinIO presigned GET against the origin hostname, which bypasses the edge cache (D12).

---

## 5. Site API and media specifics (kof22)

### 5.1 Hostnames and stages

| Hostname | Role | Initial policy | Purge |
|---|---|---|---|
| `s3.kof22.com` | MEDIA | immutable, edge TTL 1 year; bypass for every bucket except `kof22-website-files`; presigned requests (`X-Amz-Signature` in the query) always bypass | URL purge on media delete or takedown |
| `www.kof22.com`, `kof22.com` | SITE + ASSETS | Stage A: cache `/_next/static/*`, `/_next/image*` and public files only. Stage B: HTML through ISR (§6). | Path tags plus record tags |
| `api.kof22.com` | API | §3.2 public API classes. `no-store` for `/v1/me*`, `/v1/contact`, `/oauth/*` and `POST`. | Record tags |
| `admin.kof22.com`, `auth.kof22.com`, `minio-console`, DSM suite hosts | none | **Not proxied** (D4) | none |

### 5.2 Cache key normalization

The adapters ask IaC for these normalizations, and the drift view checks them:
- Ignore marketing parameters (`utm_*`, `gclid`, `fbclid`, `ref`).
- Keep every other query parameter, including Next's `_rsc`. The Next CDN guide requires this.
- Lowercase the host.
- Do not vary on cookies.
- For `/_next/image`, key on `url`, `w` and `q` only.
- Edge rules should **not** turn on "cache everything" for HTML until §6 ships. Without it, HTML stays dynamic at the edge, which is the default on most CDNs.

### 5.3 ESB mapping for kof22

Seeded `cdnTableTagRule` rows, using the 16 `PUBLISHED_TABLES` already on `kof22.cms.tables`:

| Table | Tags | URL templates (for providers without tag purge) |
|---|---|---|
| `blog_post` / `blog_post_version` | `post:{id}`, `blog:{blogId}:list`, `site:{siteId}:feeds`, `path:/blog/{blog.slug}/{slug}` | `/blog/{blog.slug}/{slug}*`, `/blog/{blog.slug}*`, `/`, `/feed.xml`, `/sitemap.xml`, `api:/v1/blog-posts/{id}*` |
| `blog` | `blog:{id}`, `blog:{id}:list` | `/blog/{slug}*` |
| `page`, `page_section` | `page:{pageId}`, `path:/{page.slug}` | `/{page.slug}*` |
| `menu`, `menu_item`, `site`, `domain`, `snippet` | `site:{siteId}` (coarse: these appear on every page) | host purge (rare) |
| `author` | `author:{id}` | `/about*`, `api:/v1/authors/{id}*` |
| `media`, `photo`, `photo_album` | `media:{id}`, `album:{albumId}` | the rendition URLs from the record; `/photography/{album.slug}*` |
| `project` (+section/update) | `project:{id}`, `path:/projects/{slug}` | `/projects/{slug}*` |
| `redirect` | `site:{siteId}:redirects` | the source path |

`blog_post_version` autosaves on drafts must not purge anything. The rule uses `publishedFilter` (the post is or was published) together with `onlyWhenFieldsChange`.

### 5.4 Site API changes (Website-Backend)

1. Replace the hard-coded `max-age` values in `finish()` with `CdnResponseHeaderPolicy`. The seeded policies reproduce today's behavior, plus `s-maxage`, `CDN-Cache-Control` and tags.
2. Add `ETag` and `If-None-Match` → 304 on anonymous reads.
3. **Drop `Vary: Origin` on anonymous GETs.** Send `Access-Control-Allow-Origin: *` there instead. This is safe because the reads are public, cookie-less and published-only. `Vary: Origin` splits the edge cache per calling origin, and some CDNs refuse to cache responses with `Vary` at all. Credentialed CORS (`/v1/me*`) keeps the allow-list and `Vary: Origin`, but those routes are `no-store` anyway.
4. Rate limiting: the Traefik limiters use `requestHost`, which is per host. Behind a CDN they would count the whole internet as one client. Switch to `ipStrategy` once real IPs are available (§7.4), and rely on edge WAF and rate limiting for anonymous floods.

---

## 6. Next.js frontend integration

### 6.1 Stage A: no rendering changes (works with any CDN)

- `/_next/static/*` already returns `immutable`, so the edge caches it as-is.
- Add `headers()` in `next.config.mjs` for `public/` files (`max-age=86400, s-maxage=604800`) and `no-store` for `/api/*`.
- Set `images.minimumCacheTTL` to `31536000`. The source images are immutable, and without this setting `/_next/image` responses get a short TTL. Each pod's on-disk optimizer cache then matters much less, because the edge becomes the shared image cache.
- `deploymentId` (version-skew protection), as the Next self-hosting guide recommends whenever a CDN is in front.

### 6.2 Image loader (optional, D7)

`images.loader: 'custom'` with `loaderFile: './cdn-image-loader.ts'`. The loader reads `NEXT_PUBLIC_IMAGE_ENGINE` and builds URLs in the same format as the backend `ImageTransformProviderInterface`:
- `RENDITIONS`: maps the width to `thumbnail` (≤ 400), `gallery` (≤ 1600) or `slideshow`
- `CLOUDFLARE_IMAGES`: `/cdn-cgi/image/width=…,quality=…,format=auto/<src>`, following the official Next example
- `IMGPROXY`: a signed URL from a small server route, because the HMAC key must stay server-side
- `NEXT_DEFAULT`: no loader.

Both sides share fixtures (§9.1), so a width maps to the same URL whether it was built by the backend or by Next.

### 6.3 Stage B: HTML caching (D8)

1. Remove `export const dynamic = 'force-dynamic'` from the root layout. Keep it only on routes that really are dynamic: `/preview` and anything that reads cookies or `searchParams` for personalization.
2. Use ISR with time-based `revalidate` (default 300 s) and `expireTime` (default 3600). Next then emits `s-maxage=300, stale-while-revalidate=3300` on those pages, as the Next CDN guide describes.
3. Tag the data fetches with `cacheTag()` (`'use cache'`) or `fetch(..., { next: { tags } })`, using the same canonical tags (`post:123`, `blog:7:list`, `site:1`), so one tag means the same thing on both sides.
4. Add a revalidation route `POST /api/revalidate` that:
   - takes `{tags: string[]}` and checks an HMAC with `CDN_REVALIDATE_SECRET`
   - calls `revalidateTag` for each tag
   - returns `200` only after revalidation has finished.
5. **Multi-pod correctness.** There are 2 replicas and no shared cache handler, so revalidating one pod leaves the other one stale. The Next self-hosting guide says the same: implement `refreshTags`, or use a shared handler. v1 handles this with `cdnRevalidationHook.fanOutDns`, which resolves the headless Service and POSTs to **every** pod before the CDN purge. A shared Redis `cacheHandler` is the alternative (D8 option c). It adds Redis, which kof22 does not currently run for the frontend.
6. Order of operations is fixed by the dispatcher: all pods revalidate, then the CDN purges by tag. Then, optionally, the `Warm` action fetches the HTML and RSC URLs.

### 6.4 Path tags for HTML

Next does not emit its internal tags as response headers in a form that can be relied on. The frontend therefore sets **path-derived tags** in `src/proxy.ts` (middleware) for cacheable routes. For example, `/blog/quick-bits/my-post` gets `path:/blog/quick-bits/my-post`, `blog-slug:quick-bits` and `site:1`. They are written in the provider's dialect, which is read from `CDN_TAG_HEADER`.

The backend's `cdnTableTagRule` produces the same path tag from `urlTemplates`, so a change to a post purges its page and its RSC variants, which share the tags. Record tags such as `post:123` on HTML are a later improvement, if Next exposes a stable way to emit them.

### 6.5 Assets on a separate hostname?

`assetPrefix` (for example `https://static.kof22.com`) is **not** needed when the CDN fronts `www` itself. It would only help with a split "media/assets CDN, HTML at origin" setup. That setup is a reasonable stopgap if D4 limits the CDN to assets, but it is not recommended.

---

## 7. Security and privacy

### 7.1 Origin protection, per provider

| Provider | Recommended | Why |
|---|---|---|
| Cloudflare | **Tunnel**: `cloudflared` runs as a Deployment (2 replicas) in `kof22-website-prod`, or in a shared `cloudflare` namespace, and connects outbound. Its ingress rules map each hostname to `https://traefik.traefik.svc:8443` (a new `cdn` entry point) with `originServerName` set, and the origin cert stays the LE wildcard. | No inbound port. The home IP is not needed in DNS for these hostnames. The `cdn` entry point is **not** port-forwarded, so the only way to reach it is the tunnel. |
| Cloudflare (no tunnel) | Port-forward plus **Authenticated Origin Pulls**, zone-level, with our own CA cert (not the global shared one), plus a Traefik `ipAllowList` of the Cloudflare ranges | The global AOP cert only proves "some Cloudflare customer", so use the zone cert |
| CloudFront / Bunny / Fastly | Port-forward plus a **secret origin header** (`X-Origin-Auth: <random>`, rotated) checked by a Traefik middleware, plus an `ipAllowList` of the provider's published ranges. A gitops CronJob refreshes the list from `ip-ranges.json` or the provider API. | These providers have no tunnel that works with a homelab origin |

### 7.2 Locking a hostname to the CDN (after a soak period)

Once a hostname has been `LIVE` and stable for 2 weeks, its IngressRoute changes from `entryPoints: [websecure]` to `entryPoints: [cdn]`. Direct requests to `50.122.5.146` with that Host header then get a 404. This is a separate, reversible gitops change, and it is **not** part of the cutover (§8.3).

Hiding the origin IP is only partial while other names, such as mail and DSM, still resolve to the same WAN IP. The design does not claim to hide it (R4).

### 7.3 Real client IP

- Trust the CDN's viewer header **only** on the `cdn` entry point:
  - Cloudflare: `CF-Connecting-IP`; the tunnel also sets `X-Forwarded-For`
  - CloudFront: `CloudFront-Viewer-Address`
  - Fastly: `Fastly-Client-IP`
  - Bunny: `CDN-ClientIP` (*verify*)
- That entry point's `forwardedHeaders.trustedIPs` is the pod CIDR (`172.24.0.0/16`, where `cloudflared` runs) and no other range. A Traefik `headers` middleware on that entry point copies the provider header into `X-Real-IP` and adds it at the right end of `X-Forwarded-For`.
- The `websecure` entry point keeps its current behavior. A spoofed `CF-Connecting-IP` sent to the port-forward is never trusted.
- Apps read **one** configured source. `IpAddresses.clientIp` (analytics), `siteapi/http/ClientIp` and the frontend `forward.ts` all switch from "count hops in XFF" to the following:
  - prefer `X-Real-IP` when the request came through the `cdn` entry point (marked by a header Traefik sets, `X-Kof22-Edge: cdn`, which is stripped from inbound requests)
  - otherwise use today's logic.
- This fixes the "Client IP hidden by proxy" (`GeoStatus.PROXY_IP`) problem **for every hostname behind the CDN**, without the paused Traefik DaemonSet / `externalTrafficPolicy: Local` change. That change is still worth doing for hostnames that are not behind the CDN (D5).

### 7.4 Privacy (analytics stays privacy-first)

- The CDN becomes a processor that sees visitor IPs and user agents. Name it on the privacy page and accept its DPA. This is an owner item in D13.
- Turn off every CDN feature that injects scripts or sets cookies. On Cloudflare that means Web Analytics / RUM beacon auto-injection, Rocket Loader, Zaraz, Email Obfuscation, and JS-challenge bot modes for normal traffic. Each one breaks the "no third-party scripts" promise or the CSP.
- `/api/e` (the analytics beacon) is `no-store` and bypassed at the edge, so every pageview still reaches the first-party collector. CDN cache hits on HTML do **not** cause analytics to be undercounted, because the beacon fires from the browser.
- The IP is still hashed with the daily salt and never stored. The CDN's own analytics, imported into `cdnEdgeStatDaily`, are aggregate counts with no IPs.

### 7.5 Credentials

- One API token per provider, with the minimum scope:
  - Cloudflare: *Zone → Cache Purge* and *Zone → Analytics: Read*, for the kof22.com zone only
  - CloudFront: `cloudfront:CreateInvalidation` and `GetInvalidation` on one distribution, plus CloudWatch `GetMetricData`
- Tokens are stored in Infisical and injected as env vars. `cdnProvider.secretRef` holds the env var name.
- **The tokens are separate from the infrastructure-as-code token** (DNS, rules), which lives only in Praetor tfvars or Infisical. The app can purge, but it cannot reconfigure the edge.

---

## 8. Infrastructure and rollout

### 8.1 DNS and certificates (depends on D1/D2)

**Cloudflare, zone moved (D2a, recommended with D1a):**
1. Create the Cloudflare zone `kof22.com` with OpenTofu (`cloudflare` provider) in Praetor `terraform/dns-cloudflare/`.
2. Add `octodns-cloudflare` as a second provider in `dns/route53/config/production.yaml`, and target `kof22.com` at Cloudflare. All the other zones stay in Route53.
3. Import every record unchanged and **DNS-only (grey cloud)**. Mail, DSM-suite, `admin`, `auth` and `minio-console` stay DNS-only permanently.
4. DNSSEC: disable the Route53 signing for `kof22.com` with tofu, wait for the DS TTL to expire, change the NS at the registrar, enable Cloudflare DNSSEC, and publish the new DS. This follows the runbook in `docs/HANDOFF-dns-plan2.md`.
5. CAA: add the CAs Cloudflare uses for edge certificates (*verify*: Cloudflare adds them automatically when CAA exists and Universal SSL is on) next to `letsencrypt.org`.
6. cert-manager: add a **Cloudflare DNS-01 solver** (scoped token *Zone → DNS: Edit*) selected for `kof22.com`, and keep Route53 for every other zone. The origin keeps the LE wildcard. Set the Cloudflare SSL mode to **Full (strict)**.

**Cloudflare, zone kept in Route53 (D2b):**
- Use Cloudflare for SaaS custom hostnames on a Cloudflare-hosted zone. Pricing and limits need checking (*verify*).
- `www`, `api` and `s3` become CNAMEs to the SaaS fallback origin.
- The **apex cannot be proxied**, because Route53 only allows ALIAS records to AWS targets at the apex. It stays direct and redirects to `www`.
- This is more complex, and it is the only option if moving DNS is off the table.

**CloudFront (D1b):**
- Route53 alias records (apex included).
- An ACM cert in `us-east-1` for `kof22.com` and `*.kof22.com`. Its DNS validation goes through the existing Route53. CAA must add `amazon.com`/`amazontrust.com`.
- One distribution per hostname group, with origin `origin.kof22.com`, a new DNS-only A record to `50.122.5.146`, and the secret origin header.
- Use the managed cache policies plus an origin request policy that forwards `CloudFront-Viewer-Address`.

### 8.2 Cluster changes (cluster-gitops, all reversible)

- Traefik: a new `cdn` entry point on `:8443`. It is ClusterIP-reachable only: not on the LoadBalancer service ports, or on the LB with no UniFi forward. Its own `forwardedHeaders.trustedIPs` is the pod CIDR.
- A `real-ip-from-cdn` headers middleware on that entry point.
- `cloudflared` Deployment (2 replicas, PDB) with the tunnel credentials from Infisical, and one ingress rule per LIVE hostname. Only for D3a.
- For a non-tunnel provider: a CronJob that refreshes an `ipAllowList` Middleware from the provider's ranges, plus an `origin-auth` middleware.
- Website-Backend and Website-Frontend env: `CDN_*` settings, revalidation secret, image engine.

### 8.3 Staged cutover per hostname

Each hostname goes through these stages, which are recorded in `cdnHostname.stage`:

1. **HEADERS_ONLY:** the policies emit headers and tags, and the purge pipeline records `SKIPPED_STAGE_OFF` rows. The e2e header matrix (§9.3) passes against the origin. Lower the DNS TTL to 60 a day ahead.
2. **LIVE (proxied):** turn on the proxy for that one record (orange cloud, or swap the Route53 alias). Watch the hit ratio, 5xx, purge latency and the analytics geo-status for 48 h.
3. **LOCKED:** after 2 weeks, bind the IngressRoute to the `cdn` entry point only (§7.2).

The order is `s3.kof22.com` → `www` Stage A (assets and images) → `api.kof22.com` → `www` Stage B (HTML). Other tenants follow (§8.5).

**Rollback** for any hostname:
- From LIVE: un-proxy the DNS record (grey cloud, or point the Route53 record back to the WAN IP). With a 60 s TTL, this takes effect in minutes. The port-forward path never went away.
- From LOCKED: first revert the IngressRoute to `websecure`.
- In the app, set `cdnHostname.stage=OFF` so purges stop.
- Header policies can stay. They are correct with or without a CDN.

### 8.4 IaC checklist (edge config owned by Praetor, D9)

- Cache rules:
  - bypass for `admin.*`, `auth.*`, `/v1/me*`, `/api/*`, `/preview*`, and any request with an `Authorization` header or a session cookie
  - respect origin headers everywhere else
  - for `s3.*/kof22-website-files/*`, edge TTL 1 year and bypass on `X-Amz-*` query strings
- Cache key normalization (§5.2).
- Strip the tag header from responses to visitors, if the provider does not do it already.
- Turn off script-injecting features (§7.4).
- WAF: managed ruleset at a sensible level, plus rate limiting on `/v1/search` and `/v1/contact`.
- The `cdnProvider.readRules` drift view compares the live edge rules against this checklist and reports any difference.

### 8.5 Other tenants

The qbit helps any **QQQ** app: the qrun.io marketing site and Benchfinity's QQQ apps can install it with their own provider rows. Non-QQQ tenants, such as galaxy.direct, kingsrook.com and mmltholdings, and static or marketing sites, can use the same infrastructure pattern (zone, tunnel, `cdn` entry point) without the qbit. They purge on deploy only, through a CI step that calls the provider API. Each zone is a separate owner decision. Nothing for other tenants is included in this spec's estimates.

---

## 9. Testing

### 9.1 Contract tests per provider (`qbit-cdn`, CI)

- An abstract **`CdnProviderContractTest`** that every adapter extends. The adapters are tested against **WireMock** stubs built from **recorded fixtures** of each provider's real API. The fixtures are recorded once by hand against a sandbox zone, with tokens scrubbed, and stored under `src/test/resources/fixtures/<provider>/`.
- What the contract checks:
  - the request shape (method, path, auth header, body)
  - batching at `itemsPerRequest`
  - that a 429 with `Retry-After` is honored and results in FAILED-then-retry
  - that a 5xx is retried
  - that a 4xx auth error goes straight to DEAD with a clear message
  - `checkHealth` success and failure
  - `importStats` parsing into `CdnEdgeStat`
  - `CdnHeaderDialect` formatting and budget collapse
  - that `signUrl` output verifies against the provider's documented algorithm
- **Shared URL fixtures:** the image URL builder and the TS loader both read `fixtures/image-urls.json` (source, w, h, q → expected URL per engine), so the backend and frontend cannot drift apart.
- **Live smoke tests** (`-Dcdn.live=true`, never in CI) run against a real zone with real tokens from the env. They purge a sentinel tag and import yesterday's stats.

### 9.2 Real cache end-to-end (CI, Testcontainers)

- **Souin** (or Varnish with `xkey`) in a container in front of a test QQQ app that has the qbit installed and the SOUIN adapter configured. The test:
  1. does a GET and checks for a MISS
  2. does the GET again and checks for a HIT (`Cache-Status`)
  3. updates the record through a QQQ action
  4. runs the ESB event (the embedded Artemis test broker from the ESB test kit) and then the dispatcher
  5. checks that the next GET is a MISS with the new content.
- The same flow runs with a fake revalidation hook, to prove the hook is called before the purge.
- Safety tests:
  - a response with `Set-Cookie` or an authenticated session is never stored
  - a `blog_post_version` autosave enqueues nothing
  - a slug change purges both the old and the new path.

### 9.3 Header matrix (Website-Backend and Website-Frontend)

A table-driven test script (`scripts/cdn-header-matrix.ts`, run in CI against the preview or staging environment, and by hand against production) checks `Cache-Control`, `CDN-Cache-Control`, the tag header (at the origin), the absence of `Set-Cookie`, and `ETag`/304 for every row in §3.2:
- `/`
- a post
- `/_next/static/...`
- `/_next/image?...`
- `/api/e`
- `/preview`
- `api /v1/blog-posts/{id}`
- `/v1/me`
- `/v1/feeds/...`
- a media rendition

After cutover, the same script runs against the edge. It normalizes `cf-cache-status`, CloudFront `x-cache` and Bunny `cdn-cache` into HIT/MISS/BYPASS, and checks that `admin.kof22.com` never shows HIT.

---

## 10. Observability and cost

- **Tables are the source of truth** for the widget and Data Health (§4.9). The qbit also exports Micrometer metrics if the app has a registry:
  - `cdn_purge_requests_total{provider,status}`
  - `cdn_purge_latency_seconds`
  - `cdn_purge_pending`
  - `cdn_stats_import_age_seconds`
- PrometheusRules in cluster-gitops `monitoring`:
  - `CdnPurgeDead` (any DEAD in 1 h, warning)
  - `CdnPurgeBacklog` (pending > 0 for 10 min)
  - `CdnHitRatioLow` (daily hit ratio below 50% for MEDIA, from the imported stats, info)
- The `cloudflared` metrics endpoint is scraped. An alert fires when its connections drop to 0, because tunnel-down means LIVE hostnames are down. The rollback runbook is linked in the alert.
- **Cost.** kof22 is estimated at 100–500 GB and about 3M requests per month:

| Option | Monthly cost |
|---|---|
| Cloudflare Free | $0 |
| CloudFront pay-as-you-go | $0 (always-free tier) |
| CloudFront flat Pro | $15 |
| Bunny | $1–5 |
| Bunny Optimizer | +$9.50 |
| Cloudflare image transforms | $0 up to 5k unique per month |
| R2 (only if D6b) | ~$0.15 for 10 GB stored, egress free |

Engineering effort is in §11.

---

## 11. Phased plan

Estimates assume one engineer working with Claude, the same assumption as the other qbit specs. Each phase ends in a deployable state.

| Phase | Scope | Estimate | Exit criteria |
|---|---|---|---|
| **P0: origin hygiene** (no CDN) | Website-Backend: `Cache-Control` on MinIO uploads plus a backfill script; Site API `ETag`/304; `Vary: Origin` cleanup. Website-Frontend: `headers()`, `images.minimumCacheTTL`, `deploymentId`. Header matrix script. | 2–3 days | The matrix passes against production. Browsers already benefit, because images are cached for a year. |
| **P1: qbit core** | New repo `QRun-IO/qbit-cdn`: SPI, `NONE` + `SOUIN` adapters, the 7 tables, header policy, tag budget, ESB trigger, dispatcher with merging, rate limit and retries, record and table actions, Health widget, Data Health rules. Core hook `QJavalinResponseHeaderCustomizerInterface` in QQQ (after the RC.4 release; targets 4.1.x). Contract and Testcontainers tests. | 8–10 days | `0.1.0` published. kof22 runs it in HEADERS_ONLY, with `SKIPPED_STAGE_OFF` rows recording the would-be purges. |
| **P2: Cloudflare adapter + infra, media first** | Cloudflare adapter (purge, health, GraphQL stats). Praetor: Cloudflare zone, octoDNS provider, DNSSEC move, CAA, cert-manager solver. cluster-gitops: `cdn` entry point, `cloudflared`, middlewares. Cut over `s3.kof22.com`. | 4–5 days, plus a DNS soak | `s3` LIVE, media hit ratio above 90% after a week |
| **P3: www Stage A + api** | Cut over `www` (assets and images only) and `api`. Purge from ESB is live. Real client IP through the `cdn` entry point. Analytics, `ClientIp` and `forward.ts` switched to the single-source rule. Traefik rate limiter switched to `ipStrategy`. | 3–4 days | Publish shows up on the API in under 30 s. Analytics shows real geo, not `PROXY_IP`. |
| **P4: www Stage B (HTML)** | Remove `force-dynamic`. ISR plus tags. `/api/revalidate`. Revalidation hook fan-out. Path tags in `proxy.ts`. Warm action. | 5–7 days | A published post is live at the edge in under 30 s. HTML hit ratio above 70%. No personalized page cached (matrix plus a manual check). |
| **P5: images + stats polish** | Image loader (D7), `ImageTransformProviderInterface` implementation, stats import tuning, drift view (rules read). | 3–4 days | Optional. Only if D7 picks transforms. |
| **P6: portability proof** (optional) | A second adapter (CloudFront or Bunny) with recorded fixtures, and an evaluation switch on a test hostname | 3–4 days | The same e2e suite passes with the second adapter |
| **LOCK** | Bind each hostname to the `cdn` entry point after 2 weeks stable | 0.5 day per hostname | Direct-origin requests for those hosts return 404 |

**Total:** about 5–7 weeks of work for P0–P4. P5 and P6 add 1–1.5 weeks. P0 can start immediately, because it does not depend on any decision.

---

## 12. Owner decisions

Each item is multiple choice, with the **recommended option first**.

| # | Decision | Options (recommended first) |
|---|---|---|
| **D1** | Which CDN goes first for kof22? | **(a) Cloudflare Free.** $0, tag/prefix/host purge on all plans, Tunnel, `CF-Connecting-IP`, image transforms with 5k free. (b) CloudFront flat-rate Pro, $15/mo. Keeps everything in Route53 and AWS, has no tag purge, and needs an inbound port plus a secret header. (c) Bunny, $1–5/mo. Cheap with good tag purge, but no tunnel and its real-IP header is unverified. (d) Self-hosted Souin or Varnish. No egress or latency benefit, so test use only. |
| **D2** | Where does `kof22.com` DNS live, if D1 = Cloudflare? | **(a) Move this one zone to Cloudflare DNS**, still managed by octoDNS in Praetor through `octodns-cloudflare`, with DNSSEC re-keyed at Cloudflare. All other zones stay in Route53. (b) Keep Route53 and use Cloudflare for SaaS custom hostnames. The apex can't be proxied, and pricing needs checking. (c) Cloudflare Business partial (CNAME) setup at about $200/mo. Not worth it. |
| **D3** | How does the CDN reach the origin? | **(a) Cloudflare Tunnel into a dedicated `cdn` Traefik entry point that is not port-forwarded.** (b) Port-forward plus zone-level Authenticated Origin Pulls plus a Cloudflare IP allowlist. (c) Port-forward plus an IP allowlist only. |
| **D4** | Which hostnames sit behind the CDN, and in what order? | **(a) `s3`, then `www` assets and images, then `api`, then `www` HTML. Never `admin`, `auth`, `minio-console` or DSM hosts.** (b) Also front `admin` for static assets only. Low value and adds risk. (c) Media only (`s3`), with HTML left at the origin. |
| **D5** | Use the CDN to solve the real-client-IP problem? | **(a) Yes, for CDN hostnames, through the trusted `cdn` entry point. Still do the paused Traefik DaemonSet / `externalTrafficPolicy: Local` change later for non-CDN hostnames.** (b) CDN header only, and drop the DaemonSet plan. (c) DaemonSet only, and ignore the CDN header. Analytics then sees CDN edge IPs, which would be worse than today. |
| **D6** | Where does media live? | **(a) Stay on MinIO, with the CDN in front and immutable headers.** (b) Move the public bucket to Cloudflare R2: zero egress, $0.015/GB-month, and fewer home-uplink fills. MinIO keeps the incoming and private buckets. Revisit if the media library grows a lot. (c) Move to AWS S3. Only if D1 = CloudFront. |
| **D7** | How are image transforms done? | **(a) Phase 1: cache Next's `/_next/image` output at the edge and keep the existing renditions (`RENDITIONS` engine for the admin and API). Add a CDN-native engine later only if needed.** (b) Cloudflare Image Transformations now, through the loader: 5k unique per month free, then $0.50 per 1k. (c) Self-hosted imgproxy now. More moving parts, but vendor-neutral. |
| **D8** | Cache HTML for `www`? | **(a) Yes, in P4: drop `force-dynamic`, use ISR plus canonical tags, and fan the revalidation out to every pod before the CDN purge.** (b) No. Keep HTML dynamic and cache only assets, images, media and the API. Simpler, and still most of the byte savings. (c) Yes, with a shared Redis `cacheHandler` instead of the fan-out. |
| **D9** | Who owns edge configuration (cache rules, WAF, TLS)? | **(a) OpenTofu in Praetor. The qbit only emits headers, purges, and reads rules for a drift view.** (b) The qbit pushes cache rules through the provider API from `cdnCachePolicy`. This gives the app reconfiguration rights, which is not recommended. |
| **D10** | Where does the code live? | **(a) New repo `QRun-IO/qbit-cdn`, plus one small generic response-header hook in QQQ core.** (b) Everything in QQQ core. (c) Only inside Website-Backend, with no reuse. |
| **D11** | Monthly budget cap for kof22's CDN | **(a) $0–25/mo.** Covers Cloudflare Free or Pro, CloudFront flat Pro, or Bunny with Optimizer. (b) Up to $50/mo. (c) No cap. |
| **D12** | Private media through signed CDN URLs in v1? | **(a) No. Private and draft media stays on MinIO presigned GETs that bypass the edge. Add `signUrl` when a real need appears.** (b) Yes. That pushes D1 toward CloudFront or Bunny, which have native signed URLs and tokens. |
| **D13** | Privacy posture | **(a) Turn off every CDN script injection and CDN analytics beacon. Name the CDN as a processor on the privacy page and accept its DPA. Keep the first-party hashed analytics as the only visitor analytics.** (b) Also enable CDN-side bot JS challenges for everyone. This adds friction and third-party JS. |
| **D14** | Purge failure policy | **(a) 5 retries with backoff, then DEAD, a Data Health issue and an alert. Content corrects itself at the edge TTL.** (b) On DEAD, automatically fall back to a hostname purge. Riskier, because it can trigger a cache stampede on a residential origin. |
| **D15** | Other tenants | **(a) kof22 only for now. Decide qrun.io and benchfinity after P4.** (b) Plan qrun.io's marketing site alongside P2. |

---

## 13. Risks

| # | Risk | Mitigation |
|---|---|---|
| R1 | A personalized or authenticated response is cached and served to others | Bypass rules at the edge, plus the `Set-Cookie`/session safety rule in the header policy (§4.6), plus matrix tests on `/v1/me`, `/preview` and `admin`. HTML caching (P4) needs explicit sign-off. |
| R2 | Stale content after a publish (purge failure, multi-pod Next) | Revalidation fan-out before the purge. DEAD alerts. Short HTML edge TTLs with `stale-while-revalidate`. Manual "Purge from CDN" action. |
| R3 | The DNS move breaks mail or DSM, or DNSSEC goes wrong | Import every record as DNS-only and diff it with octoDNS before switching NS. Follow the DNSSEC runbook with TTL waits. Do the move in a quiet window, and test MX, SPF and DKIM afterwards. |
| R4 | The origin IP is still discoverable, because other hosts share `50.122.5.146` | Don't claim it is hidden. The LOCK step blocks direct access per hostname anyway. Origin protection does not depend on secrecy. |
| R5 | Spoofed real-IP headers | Viewer headers are trusted only on the `cdn` entry point, which is reachable only through the tunnel or allowlist (§7.3). The edge marker header is stripped from inbound requests. |
| R6 | Provider terms restrict non-HTML or bulk media | kof22 serves images, not bulk video. Move video to R2 or Stream if it ever appears. Re-read the provider's current self-serve terms before P2 (*verify*). |
| R7 | Purge rate limits (Cloudflare Free: 5 tag purges per minute) | Merging and per-provider token buckets. One request carries up to 100 tags. Upgrade to Pro (5 per second) only if the backlog alert fires. |
| R8 | Lock-in through provider-specific rules | Rules live in IaC, the app only uses canonical headers and tags, and P6 proves a second adapter. |
| R9 | The tunnel is a single point of failure | Two `cloudflared` replicas with a PDB and an alert. Rollback is a DNS un-proxy, because the port-forward still exists until LOCK. |
| R10 | Release interference | No QQQ core change lands before RC.4 is cut. The core hook goes to the next 4.1.x. |

---

## 14. Sources

Researched 2026-10-12. Prices change often.

- Cloudflare purge (all plans, rate limits): https://developers.cloudflare.com/cache/how-to/purge-cache/ ; tags: https://developers.cloudflare.com/cache/how-to/purge-cache/purge-by-tags/
- Cloudflare Cache Rules / Tiered Cache: https://developers.cloudflare.com/cache/how-to/cache-rules/ , https://developers.cloudflare.com/cache/how-to/tiered-cache/
- Cloudflare `CDN-Cache-Control`: https://blog.cloudflare.com/cdn-cache-control/
- Cloudflare Images transformations and pricing: https://developers.cloudflare.com/images/optimization/features/ , https://developers.cloudflare.com/images/pricing/
- Cloudflare R2 pricing: https://developers.cloudflare.com/r2/pricing/
- Cloudflare Authenticated Origin Pulls: https://developers.cloudflare.com/ssl/origin-configuration/authenticated-origin-pull/ ; Tunnel: https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/ ; IPs: https://www.cloudflare.com/ips-v4 ; headers: https://developers.cloudflare.com/fundamentals/reference/http-headers/ ; GraphQL Analytics: https://developers.cloudflare.com/analytics/graphql-api/ ; plans: https://www.cloudflare.com/plans/
- CloudFront flat-rate plans: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/flat-rate-pricing-plan.html , https://aws.amazon.com/blogs/networking-and-content-delivery/introducing-flat-rate-pricing-plans-with-no-overages/
- CloudFront pricing: https://aws.amazon.com/cloudfront/pricing/ ; invalidation: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/PayingForInvalidation.html ; private content: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/PrivateContent.html ; edge functions: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/edge-functions-choosing.html ; origin shield: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/origin-shield.html ; edge locations and prefix lists: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/LocationsOfEdgeServers.html ; custom origin headers: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/add-origin-custom-headers.html ; viewer headers: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/adding-cloudfront-headers.html ; real-time logs: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/real-time-logs.html
- Fastly Surrogate-Key: https://www.fastly.com/documentation/reference/http/http-headers/Surrogate-Key/ ; purging: https://www.fastly.com/documentation/guides/full-site-delivery/purging/getting-started-with-purging/ ; batch purge: https://www.fastly.com/blog/introducing-batch-api-surrogate-key-purge ; client IP: https://www.fastly.com/documentation/reference/http/http-headers/Fastly-Client-IP/ ; pricing: https://www.fastly.com/pricing
- Bunny purge: https://bunny.net/docs/cdn/purge-cache , https://bunny.net/docs/api-reference/core/pull-zone/purge-cache , https://bunny.net/blog/introducing-tag-based-cdn-cache-purging/ ; token auth: https://bunny.net/docs/cdn/security/token-authentication/advanced ; pricing: https://bunny.net/pricing/cdn/
- Akamai cache tags: https://techdocs.akamai.com/purge-cache/docs/purge-content-cache-tag , https://techdocs.akamai.com/purge-cache/reference/api ; targeted cache control: https://www.akamai.com/blog/news/targeted-cache-control
- Azure Front Door: https://learn.microsoft.com/en-us/azure/frontdoor/front-door-cdn-comparison , https://learn.microsoft.com/en-us/azure/frontdoor/cache-purge , https://azure.microsoft.com/en-us/pricing/details/cdn/
- Google Cloud CDN invalidation (incl. cache tags): https://docs.cloud.google.com/cdn/docs/cache-invalidation-overview ; pricing: https://cloud.google.com/cdn/pricing ; external backends: https://docs.cloud.google.com/cdn/docs/ext-backend-internet-neg-overview ; signed URLs: https://docs.cloud.google.com/cdn/docs/using-signed-urls
- Varnish xkey: https://docs.varnish-software.com/book/invalidation/secondary-keys/ ; Souin: https://github.com/darkweak/souin ; Traefik Hub HTTP cache: https://doc.traefik.io/traefik-hub/api-gateway/reference/routing/http/middlewares/ref-httpcache ; nginx `proxy_cache_purge`: https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_cache_purge
- imgproxy signing: https://docs.imgproxy.net/usage/signing_url ; presets: https://docs.imgproxy.net/usage/presets ; thumbor: https://thumbor.readthedocs.io/
- RFC 9111: https://www.rfc-editor.org/rfc/rfc9111 ; RFC 9110: https://www.rfc-editor.org/rfc/rfc9110 ; RFC 9213: https://httpwg.org/specs/rfc9213.html ; RFC 5861: https://www.rfc-editor.org/rfc/rfc5861 ; W3C Edge Architecture: https://www.w3.org/TR/edge-arch/ ; cache groups draft: https://datatracker.ietf.org/doc/draft-ietf-httpbis-cache-groups/
- Next.js self-hosting: https://nextjs.org/docs/app/guides/self-hosting ; CDN caching: https://nextjs.org/docs/app/guides/cdn-caching ; `expireTime`: https://nextjs.org/docs/app/api-reference/config/next-config-js/expireTime ; images and loaders: https://nextjs.org/docs/app/api-reference/config/next-config-js/images
- Traefik forwarded headers: https://doc.traefik.io/traefik/routing/entrypoints/#forwarded-headers
- Internal (kof22): Website-Backend `docs/site-api.md`, `docs/cms-media.md`, `metadata/esb/WebsiteEsbMetaData`, `siteapi/http/SiteApiRouteProvider`; Website-Frontend `next.config.mjs`, `src/app/layout.tsx`, `src/lib/analytics/forward.ts`; cluster-gitops `tenants/kof22/website/*`, `platform/traefik/values.yaml`; Praetor `dns/route53/zones/kof22.com.yaml`, `docs/STATUS-2026-10-11-requests.md`; QQQ `docs/specs/2026-09-25-qqq-esb-design.md`.
